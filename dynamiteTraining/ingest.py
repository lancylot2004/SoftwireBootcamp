from concurrent.futures import ProcessPoolExecutor
import joblib
from pathlib import Path
import numpy as np
import orjson
from tqdm import tqdm

from structures import Game, GameSnapshot, Move, PlayerSnapshot

MOVES = Move._member_map_.values()
BATCH_SIZE = 1000


def parse_json(json_path: Path) -> Game:
    moves = orjson.loads(json_path.read_bytes())["moves"]
    snapshots = []
    p1_counts = {m: 0.0 for m in MOVES}
    p2_counts = {m: 0.0 for m in MOVES}
    p1_dynamite = p2_dynamite = 100
    p1_since_dynamite = p2_since_dynamite = p1_since_water = p2_since_water = np.inf
    rollover = 0

    for move in moves:
        p1, p2 = Move(move["p1"]), Move(move["p2"])
        p1_counts[p1] += 1
        p2_counts[p2] += 1
        rollover = 0 if p1 != p2 else rollover + 1

        p1_dynamite -= int(p1 == Move.DYNAMITE)
        p2_dynamite -= int(p2 == Move.DYNAMITE)
        p1_since_dynamite = 0 if p1 == Move.DYNAMITE else p1_since_dynamite + 1
        p2_since_dynamite = 0 if p2 == Move.DYNAMITE else p2_since_dynamite + 1
        p1_since_water = 0 if p1 == Move.WATER else p1_since_water + 1
        p2_since_water = 0 if p2 == Move.WATER else p2_since_water + 1

        total = p1_counts[p1] + p2_counts[p2]
        p1_probs = {m: p1_counts[m] / total for m in Move}
        p2_probs = {m: p2_counts[m] / total for m in Move}

        snapshots.append(GameSnapshot(
            rollover = rollover,
            player_one = PlayerSnapshot(
                move = p1,
                dynamite_left = p1_dynamite,
                rounds_since_dynamite = p1_since_dynamite,
                rounds_since_water = p1_since_water,
                move_probabilities = p1_probs
            ),
            player_two = PlayerSnapshot(
                move = p2,
                dynamite_left = p2_dynamite,
                rounds_since_dynamite = p2_since_dynamite,
                rounds_since_water = p2_since_water,
                move_probabilities = p2_probs
            )
        ))

    return Game(moves=snapshots)


def save_snapshots_batch(snapshots: list[Game], out_dir: Path, batch_idx: int):
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / f"part-{batch_idx:04d}.dump"
    joblib.dump(snapshots, out_file, compress=0)


def process_directory(json_dir: Path, out_dir: Path, max_workers: int = None):
    all_json = sorted(json_dir.rglob("*.json"))
    total_files = len(all_json)
    batch = []
    batch_idx = 0

    with ProcessPoolExecutor(max_workers = max_workers) as executor:
        for game in tqdm(
            executor.map(parse_json, all_json),
            total=total_files,
            desc="Parsing JSON",
            unit="files"
        ):
            batch.append(game)
            if len(batch) == BATCH_SIZE:
                save_snapshots_batch(batch, out_dir, batch_idx)
                batch_idx += 1
                batch = []

    if batch:
        save_snapshots_batch(batch, out_dir, batch_idx)


if __name__ == "__main__":
    base_dir = Path("history/up-to-2000")
    output_dir = Path("dumps/up-to-2000")
    process_directory(base_dir, output_dir, max_workers = 64)
