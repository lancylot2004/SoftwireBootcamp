from enum import Enum
import numpy as np
from dataclasses import dataclass
from typing import List, Dict, Set


MAX_ROLLOVER = 1000
MAX_GAME_LENGTH = 2500
MAX_DYNAMITE = 100

class Move(Enum):
    ROCK = 'R'
    PAPER = 'P'
    SCISSORS = 'S'
    DYNAMITE = 'D'
    WATER = 'W'

    def beats(self, other: 'Move') -> bool:
        if self == other:
            return False
        return other in {
            Move.ROCK: {Move.SCISSORS, Move.DYNAMITE},
            Move.PAPER: {Move.ROCK, Move.DYNAMITE},
            Move.SCISSORS: {Move.PAPER, Move.DYNAMITE},
            Move.DYNAMITE: {Move.ROCK, Move.PAPER, Move.SCISSORS},
            Move.WATER: {Move.DYNAMITE}
        }[self]

    def beaten_by(self) -> Set['Move']:
        return {
            Move.ROCK: {Move.PAPER, Move.DYNAMITE},
            Move.PAPER: {Move.SCISSORS, Move.DYNAMITE},
            Move.SCISSORS: {Move.ROCK, Move.DYNAMITE},
            Move.DYNAMITE: {Move.WATER},
            Move.WATER: {Move.ROCK, Move.PAPER, Move.SCISSORS}
        }[self]

    def one_hot(self) -> np.ndarray:
        vec = np.zeros(len(Move), dtype=np.float32)
        vec[self.index()] = 1.0
        return vec

    def index(self) -> int:
        return list(Move).index(self)

    @staticmethod
    def from_index(idx: int) -> 'Move':
        return list(Move)[idx]


@dataclass(frozen = True)
class PlayerSnapshot:
    move: Move
    dynamite_left: int
    rounds_since_dynamite: int
    rounds_since_water: int
    move_probabilities: Dict[Move, float]

    def aggregate(self, rollover: float) -> np.ndarray:
        rollover_norm = min(rollover / MAX_ROLLOVER, 1.0)
        dynamite_left_norm = self.dynamite_left / 100.0
        rounds_since_dynamite_norm = min(self.rounds_since_dynamite / MAX_GAME_LENGTH, 1.0)
        rounds_since_water_norm = min(self.rounds_since_water / MAX_GAME_LENGTH, 1.0)

        return np.concatenate([
            [rollover_norm, dynamite_left_norm, rounds_since_dynamite_norm, rounds_since_water_norm],
            self.move.one_hot(),
            np.array([
                self.move_probabilities.get(move, 0.0) for move in Move
            ], dtype=np.float32)
        ])
    

@dataclass(frozen = True)
class GameSnapshot:
    rollover: int
    player_one: PlayerSnapshot
    player_two: PlayerSnapshot


@dataclass(frozen = True)
class Game:
    moves: List[GameSnapshot]


@dataclass(frozen=True)
class Bot:
    id: int
    name: str
    file_type: str
    user_id: int
    username: str
    created_at: str
    updated_at: str
