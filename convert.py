import sys
import tomllib
import json
import time

start = time.time()
with open("Cargo.lock", "rb") as f:
    data = tomllib.load(f)

data = json.dumps(data)
print(data[:20])
print(1000 * (time.time() - start), "ms")
