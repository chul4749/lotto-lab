#!/usr/bin/env python3
"""로또 6/45 당첨번호를 동행복권에서 받아 draws.json 에 이어 붙인다.

이미 있는 회차는 두고, 그 다음 회차부터 새로 나온 것만 받는다.
사용: python3 tools/lotto_draws.py draws.json
"""
import json
import os
import sys
import time
import urllib.request

URL = "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={}"


def fetch(no):
    req = urllib.request.Request(URL.format(no), headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=15) as res:
        d = json.loads(res.read().decode("utf-8"))
    if d.get("returnValue") != "success":
        return None
    return {
        "no": d["drwNo"],
        "date": d["drwNoDate"],
        "nums": sorted(d["drwtNo{}".format(i)] for i in range(1, 7)),
        "bonus": d["bnusNo"],
        "prize": d.get("firstWinamnt", 0),
        "winners": d.get("firstPrzwnerCo", 0),
    }


def main():
    path = sys.argv[1]
    draws = []
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            draws = json.load(f).get("draws", [])
    no = draws[-1]["no"] + 1 if draws else 1
    added = 0
    while True:
        try:
            d = fetch(no)
        except Exception as e:  # 막히거나 JSON이 아니면 여기까지만 받고 다음 실행에 잇는다
            print("{}회 받기 실패: {}".format(no, e))
            break
        if d is None:
            break
        draws.append(d)
        added += 1
        no += 1
        time.sleep(0.05)
    if added:
        with open(path, "w", encoding="utf-8") as f:
            json.dump({"updated": time.strftime("%Y-%m-%dT%H:%M:%S%z"), "draws": draws}, f, ensure_ascii=False, separators=(",", ":"))
    print("{}회까지, 이번에 {}회 추가".format(draws[-1]["no"] if draws else 0, added))
    if not draws:
        sys.exit(1)


if __name__ == "__main__":
    main()
