import joblib
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import numpy as np
from pathlib import Path
from typing import List
from tqdm import tqdm

from structures import MAX_DYNAMITE, MAX_ROLLOVER, Game, GameSnapshot, Move

DEVICE = torch.device("mps" if torch.backends.mps.is_available() else "cpu")
BATCH_SIZE = 128
WINDOW_SIZE = 50
FEATURE_SIZE = 28
STATE_SIZE = 3
NUM_CLASSES = len(Move)
DUMMY_HISTORY = torch.randn(BATCH_SIZE, WINDOW_SIZE, FEATURE_SIZE).to(DEVICE)
DUMMY_STATE = torch.randn(BATCH_SIZE, STATE_SIZE).to(DEVICE)


class DynamiteTransformerNet(nn.Module):
    def __init__(self, feature_size: int, state_size: int, num_classes: int) -> None:
        super().__init__()
        self.embedding_dim = 32

        encoder_layer = nn.TransformerEncoderLayer(
            d_model=self.embedding_dim,
            nhead=2,
            dim_feedforward=64,
            dropout=0.2,
            batch_first=True
        )
        self.transformer = nn.TransformerEncoder(encoder_layer, num_layers=1)
        self.input_fc = nn.Linear(feature_size, self.embedding_dim)

        self.state_fc = nn.Sequential(
            nn.Linear(state_size, 32),
            nn.ReLU(),
            nn.LayerNorm(32),
            nn.Dropout(0.2)
        )

        self.combined_fc = nn.Sequential(
            nn.Linear(self.embedding_dim + 32, 64),
            nn.ReLU(),
            nn.LayerNorm(64),
            nn.Dropout(0.2),
            nn.Linear(64, num_classes)
        )

    def forward(self, history_input: torch.Tensor, state_input: torch.Tensor) -> torch.Tensor:
        x = self.input_fc(history_input)
        x = self.transformer(x)  # [batch, seq_len, embedding_dim]
        x = x.mean(dim=1)

        state_features = self.state_fc(state_input)
        combined = torch.cat((x, state_features), dim=1)
        return self.combined_fc(combined)


class DynamiteDataset(Dataset):
    def __init__(self, snapshots, window_size):
        self.snapshots = snapshots
        self.window_size = window_size

    def __len__(self):
        return len(self.snapshots) - self.window_size

    def __getitem__(self, idx):
        window = self.snapshots[idx:idx + self.window_size]
        hist = torch.stack([
            torch.tensor(np.concatenate([
                snap.player_one.aggregate(snap.rollover),
                snap.player_two.aggregate(snap.rollover)
            ]), dtype=torch.float32)
            for snap in window
        ])  # [window_size, feature_size]

        last_snap = self.snapshots[idx + self.window_size]
        state = torch.tensor(np.concatenate([
            [last_snap.player_one.dynamite_left / MAX_DYNAMITE],
            [last_snap.player_two.dynamite_left / MAX_DYNAMITE],
            [last_snap.rollover / MAX_ROLLOVER],
        ]), dtype=torch.float32).flatten()

        label = torch.tensor(last_snap.player_one.move.index(), dtype=torch.long)
        return hist, state, label


def train_model(
    snapshots: List[GameSnapshot],
    path: str,
    window_size: int = WINDOW_SIZE,
    feature_size: int = FEATURE_SIZE,
    state_size: int = STATE_SIZE,
    num_classes: int = NUM_CLASSES,
    epochs: int = 10,
    batch_size: int = BATCH_SIZE,
    lr: float = 1e-3
) -> DynamiteTransformerNet:
    model = DynamiteTransformerNet(feature_size, state_size, num_classes).to(DEVICE)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=1e-2)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=epochs)

    dataset = DynamiteDataset(snapshots, window_size)
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=True, num_workers=4)

    best_loss = float('inf')

    for epoch in range(epochs):
        model.train()
        total_loss = 0.0

        for hist_batch, state_batch, label_batch in tqdm(loader, desc=f"Epoch {epoch + 1}/{epochs}"):
            hist_batch = hist_batch.to(DEVICE, non_blocking=True)
            state_batch = state_batch.to(DEVICE, non_blocking=True)
            label_batch = label_batch.to(DEVICE, non_blocking=True)

            optimizer.zero_grad()

            outputs = model(hist_batch, state_batch)
            loss = criterion(outputs, label_batch)

            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
            optimizer.step()

            total_loss += loss.item()

        scheduler.step()

        avg_loss = total_loss / len(loader)
        print(f"Epoch [{epoch + 1}/{epochs}] Loss: {avg_loss:.4f}")


        if avg_loss < best_loss:
            best_loss = avg_loss
            torch.save(model.state_dict(), f"{path}-best.pth")

        torch.onnx.export(
            model,
            (DUMMY_HISTORY, DUMMY_STATE),
            f"{path}-{epoch + 1}.onnx",
            input_names=["history_input", "state_input"],
            output_names=["output"],
            dynamic_axes={
                "history_input": {0: "batch_size", 1: "window_size"},
                "state_input": {0: "batch_size"},
                "output": {0: "batch_size"}
            },
            opset_version=14
        )

    return model

if __name__ == "__main__":
    base_dir = Path("dumps")
    output_path = "models/dynamite_transformer"

    snapshots: List[Game] = joblib.load(base_dir / "above-2000.dump")
    snapshots = [snap for game in snapshots for snap in game.moves]
    print(f"[Load] {len(snapshots)} snapshots.")

    model = train_model(
        snapshots=snapshots,
        path=output_path,
        window_size=WINDOW_SIZE,
        feature_size=FEATURE_SIZE,
        state_size=STATE_SIZE,
        num_classes=NUM_CLASSES,
        epochs=10,
        batch_size=BATCH_SIZE,
        lr=1e-3
    )
