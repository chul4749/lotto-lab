#!/usr/bin/env python3
"""로또 6/45 당첨번호를 받아 draws.json 에 이어 붙인다.

동행복권은 해외(GitHub Actions) 접속을 막아서, 동행복권 결과를 공개 JSON으로 모아 두는
smok95/lotto 를 먼저 받고, 거기 아직 없는 회차만 동행복권에 직접 물어본다.
사용: python3 tools/lotto_draws.py draws.json
"""
import json
import os
import sys
import time
import urllib.request

URL = "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={}"
MIRROR = "https://raw.githubusercontent.com/smok95/lotto/main/results/all.json"


def fetch_mirror():
    req = urllib.request.Request(MIRROR, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=60) as res:
        rows = json.loads(res.read().decode("utf-8"))
    out = []
    for r in rows:
        nums = sorted(r["numbers"])
        if len(nums) != 6 or not all(1 <= n <= 45 for n in nums + [r["bonus_no"]]):
            raise ValueError("{}회 번호가 이상함".format(r.get("draw_no")))
        div = r.get("divisions") or []
        first = div[0] if div else {}
        out.append({
            "no": r["draw_no"],
            "date": r["date"][:10],
            "nums": nums,
            "bonus": r["bonus_no"],
            "prize": first.get("prize", 0),
            "winners": first.get("winners", 0),
            "prizes": [(div[i] or {}).get("prize", 0) if i < len(div) else 0 for i in range(5)],
        })
    return sorted(out, key=lambda d: d["no"])


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
    added = 0
    try:
        have = draws[-1]["no"] if draws else 0
        new = [d for d in fetch_mirror() if d["no"] > have]
        # 회차가 빠짐없이 이어질 때만 붙인다
        for d in new:
            if d["no"] != (draws[-1]["no"] if draws else 0) + 1:
                break
            draws.append(d)
            added += 1
    except Exception as e:
        print("smok95/lotto 받기 실패: {}".format(e))
    no = draws[-1]["no"] + 1 if draws else 1
    while True:
        try:
            d = fetch(no)
        except Exception as e:  # 막히거나 JSON이 아니면 여기까지만 받고 다음 실행에 잇는다
            print("동행복권 {}회 받기 실패: {}".format(no, e))
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
