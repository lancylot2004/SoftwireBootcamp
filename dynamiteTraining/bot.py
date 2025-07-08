import sys
import subprocess
import tempfile
import base64
import os

MODEL_B64 = """<REPLACE_WITH_BASE64_MODEL>"""

def ensure_dependencies():
    try:
        import onnxruntime
        import numpy as np
    except ImportError:
        if sys.platform == "win32":
            DETACHED_PROCESS = 0x00000008
            subprocess.Popen(
                [sys.executable, "-m", "pip", "install", "onnxruntime", "numpy"],
                creationflags=subprocess.CREATE_NEW_PROCESS_GROUP | DETACHED_PROCESS
            )
        else:
            subprocess.Popen(
                [sys.executable, "-m", "pip", "install", "onnxruntime", "numpy"],
                preexec_fn=os.setsid,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL
            )

ensure_dependencies()
import onnxruntime
import numpy as np

model_path = tempfile.NamedTemporaryFile(delete=False, suffix=".onnx").name
with open(model_path, "wb") as f:
    f.write(base64.b64decode(MODEL_B64))

MAX_DYNAMITE = 100
MAX_ROLLOVER = 1000
MAX_GAME_LENGTH = 2500
MOVES = ['R', 'P', 'S', 'D', 'W']


class PaperBot:
    def __init__(self, window_size=50):
        self.window_size = window_size
        self.session = onnxruntime.InferenceSession(model_path)
        self.history_input_name = self.session.get_inputs()[0].name
        self.state_input_name = self.session.get_inputs()[1].name

    def make_move(self, gamestate):
        rounds = gamestate.get('rounds', [])
        snapshots = self._generate_snapshots(rounds)

        history, state = self._prepare_inputs(snapshots)

        inputs = {
            self.history_input_name: history,
            self.state_input_name: state
        }
        outputs = self.session.run(None, inputs)
        logits = outputs[0][0]  # Shape: [num_classes]
        predicted_idx = int(np.argmax(logits))
        return MOVES[predicted_idx]

    def _generate_snapshots(self, rounds):
        snapshots = []
        p1_dynamite_left = MAX_DYNAMITE
        p2_dynamite_left = MAX_DYNAMITE
        p1_since_dynamite = 0
        p2_since_dynamite = 0
        p1_since_water = 0
        p2_since_water = 0
        rollover = 0

        for r in rounds:
            p1_move = r['p1']
            p2_move = r['p2']

            if p1_move == 'D':
                p1_dynamite_left = max(0, p1_dynamite_left - 1)
                p1_since_dynamite = 0
            else:
                p1_since_dynamite += 1

            if p2_move == 'D':
                p2_dynamite_left = max(0, p2_dynamite_left - 1)
                p2_since_dynamite = 0
            else:
                p2_since_dynamite += 1

            p1_since_water = 0 if p1_move == 'W' else p1_since_water + 1
            p2_since_water = 0 if p2_move == 'W' else p2_since_water + 1
            rollover = min(rollover + 1, MAX_ROLLOVER) if p1_move == p2_move else 0

            snapshots.append({
                'rollover': rollover,
                'p1_move': p1_move,
                'p2_move': p2_move,
                'p1_dynamite': p1_dynamite_left,
                'p2_dynamite': p2_dynamite_left,
                'p1_since_dynamite': p1_since_dynamite,
                'p2_since_dynamite': p2_since_dynamite,
                'p1_since_water': p1_since_water,
                'p2_since_water': p2_since_water
            })

        return snapshots

    def _prepare_inputs(self, snapshots):
        history_features = []
        for snap in snapshots[-self.window_size:]:
            features = self._combined_features(snap)
            history_features.append(features)

        pad_count = self.window_size - len(history_features)
        if pad_count > 0:
            history_features = [self._neutral_features()] * pad_count + history_features

        history_array = np.array(history_features, dtype=np.float32).reshape(1, self.window_size, -1)

        last_snap = snapshots[-1] if snapshots else self._neutral_snapshot()
        state_array = np.array(self._state_features(last_snap), dtype=np.float32).reshape(1, -1)

        return history_array, state_array

    def _combined_features(self, snap):
        p1_one_hot = self._one_hot_move(snap['p1_move'])
        p2_one_hot = self._one_hot_move(snap['p2_move'])
        return np.concatenate([
            [snap['rollover'] / MAX_ROLLOVER],
            [snap['p1_dynamite'] / MAX_DYNAMITE, snap['p2_dynamite'] / MAX_DYNAMITE],
            [snap['p1_since_dynamite'] / MAX_GAME_LENGTH, snap['p2_since_dynamite'] / MAX_GAME_LENGTH],
            [snap['p1_since_water'] / MAX_GAME_LENGTH, snap['p2_since_water'] / MAX_GAME_LENGTH],
            p1_one_hot,
            p2_one_hot
        ])

    def _state_features(self, snap):
        return [
            snap['p1_dynamite'] / MAX_DYNAMITE,
            snap['p2_dynamite'] / MAX_DYNAMITE,
            snap['rollover'] / MAX_ROLLOVER
        ]

    def _one_hot_move(self, move):
        vec = np.zeros(len(MOVES), dtype=np.float32)
        if move in MOVES:
            vec[MOVES.index(move)] = 1.0
        return vec

    def _neutral_features(self):
        return self._combined_features(self._neutral_snapshot())

    def _neutral_snapshot(self):
        return {
            'rollover': 0,
            'p1_move': 'R',
            'p2_move': 'R',
            'p1_dynamite': MAX_DYNAMITE,
            'p2_dynamite': MAX_DYNAMITE,
            'p1_since_dynamite': 0,
            'p2_since_dynamite': 0,
            'p1_since_water': 0,
            'p2_since_water': 0
        }


import atexit
atexit.register(lambda: os.remove(model_path))
