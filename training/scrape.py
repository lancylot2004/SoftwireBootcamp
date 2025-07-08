from concurrent.futures import ThreadPoolExecutor, as_completed
import threading
import time
import requests
import json
import random
from dataclasses import asdict
from pathlib import Path
from typing import List

from config import EMAIL, PASSWORD
from structures import Bot

BASE_URL = "https://dynamite.softwire.com"
MAX_RETRIES = 3
BASE_RETRY = 1
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:140.0) Gecko/20100101 Firefox/140.0",
    "Content-Type": "application/json",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "en-GB,en;q=0.5",
    "Accept-Encoding": "gzip, deflate, br, zstd",
    "DNT": "1",
    "Sec-GPC": "1",
    "Connection": "keep-alive",
    "Origin": BASE_URL,
}


def login(email: str, password: str) -> str:
    headers = HEADERS.copy()
    headers.update({ "Referer": f"{BASE_URL}/signIn" })
    payload = {
        "existingUserEmail": email,
        "existingUserPassword": password
    }
    response = requests.post(f"{BASE_URL}/api/login", json = payload, headers = headers)
    response.raise_for_status()
    cookies = response.cookies.get_dict()
    session_id = cookies.get("connect.sid")
    if not session_id:
        raise ValueError("Login failed: no session cookie found")
    return session_id

def fetch_all_bots(session_id: str) -> List[Bot]:
    headers = HEADERS.copy()
    headers.update({
        "Referer": f"{BASE_URL}/bots",
        "Cookie": f"connect.sid={session_id}",
    })
    bots = []
    page = 1
    while True:
        params = {"page": page, "orderBy": "updatedAt", "direction": "DESC"}
        response = requests.get(f"{BASE_URL}/bots", headers = headers, params = params)
        response.raise_for_status()
        data = response.json()
        for bot_data in data.get("bots", []):
            bot = Bot.from_dict(bot_data)
            bots.append(bot)
        if page >= data.get("totalPages", 1):
            break
        page += 1
    return bots

def save_bots_to_file(bots: List[Bot], filename: str):
    with open(filename, "w") as f:
        json.dump([asdict(bot) for bot in bots], f, indent = 4)

def load_bots_from_file(filename: str) -> List[Bot]:
    with open(filename, "r") as f:
        data = json.load(f)
        return [Bot.from_dict(bot) for bot in data]

def play_bots(session_id: str, bot_id1: int, bot_id2: int) -> dict:
    headers = HEADERS.copy()
    headers.update({
        "Referer": f"{BASE_URL}/bots",
        "Cookie": f"connect.sid = {session_id}",
    })
    payload = {"botId1": bot_id1, "botId2": bot_id2}
    response = requests.post(f"{BASE_URL}/play", json = payload, headers = headers)
    response.raise_for_status()
    return response.json()

def run_random_matches(session_id: str, bots: List[Bot], output_dir: str, how_many: int = 10):
    Path(output_dir).mkdir(parents = True, exist_ok = True)

    for _ in range(how_many):
        bot1, bot2 = random.sample(bots, 2)
        filename = Path(output_dir) / f"{bot1.id}-{bot2.id}.json"
        if filename.exists():
            print(f"Skipping existing match result: {filename}")
            continue
        print(f"Playing {bot1.name} vs {bot2.name}")
        result = play_bots(session_id, bot1.id, bot2.id)
        with open(filename, "w") as f:
            json.dump(result, f, indent=4)
        print(f"Saved match result to {filename}")


def fetch_match_moves(session_id: str, match_id: int, output_dir: str, stop_event: threading.Event):
    if stop_event.is_set():
        return False

    headers = {
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:140.0)",
        "Accept": "application/json, text/plain, */*",
        "Referer": "https://dynamite.softwire.com/bots",
        "Cookie": f"connect.sid={session_id}",
    }
    url = f"https://dynamite.softwire.com/api/matchResults/{match_id}/moves.json"
    output_file = Path(output_dir) / f"{match_id}.json"

    if output_file.exists():
        print(f"Skipping match {match_id}, file already exists.")
        return True

    attempts = 0
    while attempts < MAX_RETRIES:
        try:
            response = requests.get(url, headers=headers, timeout=10)
            if response.status_code == 404:
                print(f"Match {match_id} not found (404). Stopping all threads.")
                stop_event.set()
                return False

            response.raise_for_status()
            data = response.json()

            with open(output_file, "w") as f:
                json.dump(data, f, indent = 4)
            print(f"Saved moves for match {match_id} to {output_file}")
            return True

        except requests.RequestException as e:
            attempts += 1
            if attempts < MAX_RETRIES:
                wait_time = BASE_RETRY * attempts
                print(f"Error fetching match {match_id}: {e}. Retrying in {wait_time}s ({attempts}/{MAX_RETRIES})...")
                time.sleep(wait_time)
            else:
                print(f"Failed to fetch match {match_id} after {MAX_RETRIES} attempts.")
                return False

def fetch_all_match_moves_parallel(session_id: str, output_dir: str, num_threads: int):
    Path(output_dir).mkdir(parents = True, exist_ok = True)
    stop_event = threading.Event()
    match_id = 1

    with ThreadPoolExecutor(max_workers = num_threads) as executor:
        futures = {}
        while not stop_event.is_set():
            for _ in range(num_threads):
                future = executor.submit(fetch_match_moves, session_id, match_id, output_dir, stop_event)
                futures[future] = match_id
                match_id += 1

            for future in as_completed(futures):
                if stop_event.is_set():
                    break
                future.result()
            futures.clear()


if __name__ == "__main__":
    session = login(EMAIL, PASSWORD)
    # bots = fetch_all_bots(session)
    # save_bots_to_file(bots, bots_file)
    # bots = load_bots_from_file(bots_file)
    # run_random_matches(session, bots, matches_dir, 100)
    fetch_all_match_moves_parallel(session, "history", num_threads = 32)

