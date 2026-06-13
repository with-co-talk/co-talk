#!/usr/bin/env python3
"""
SMTP 테스트 발송 (표준 라이브러리만 사용).

예:
  cd co-talk && python3 scripts/smtp_send_test.py you@example.com
  python3 scripts/smtp_send_test.py --env-file /path/to/.env you@example.com

필요 환경변수: MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD
선택: MAIL_PORT(기본 587), MAIL_FROM_ADDRESS(기본 MAIL_USERNAME과 동일)
"""
from __future__ import annotations

import argparse
import os
import smtplib
import ssl
import sys
from email.mime.text import MIMEText
from pathlib import Path


def load_dotenv(path: str | Path) -> None:
    """주석/# 제외, KEY=VALUE 만 로드. 이미 설정된 키는 덮어쓰지 않음."""
    p = Path(path)
    if not p.is_file():
        print(f"No such file: {p}", file=sys.stderr)
        sys.exit(2)
    for raw in p.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key, value = key.strip(), value.strip()
        if key and key not in os.environ:
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
                value = value[1:-1]
            os.environ[key] = value


def main() -> None:
    ap = argparse.ArgumentParser(description="Send one SMTP test message.")
    ap.add_argument("to", help="수신 이메일 주소")
    ap.add_argument("--env-file", help="dotenv-style file (e.g. project .env)")
    args = ap.parse_args()

    if args.env_file:
        load_dotenv(args.env_file)

    host = (os.environ.get("MAIL_HOST") or "").strip()
    port_s = (os.environ.get("MAIL_PORT") or "587").strip()
    user = (os.environ.get("MAIL_USERNAME") or "").strip()
    password = os.environ.get("MAIL_PASSWORD") or ""
    from_addr = (os.environ.get("MAIL_FROM_ADDRESS") or "").strip() or user

    missing = []
    if not host:
        missing.append("MAIL_HOST")
    if not user:
        missing.append("MAIL_USERNAME")
    if not password:
        missing.append("MAIL_PASSWORD")
    if missing:
        print("Missing: " + ", ".join(missing), file=sys.stderr)
        print("Use --env-file 또는 셸에서 export MAIL_* 후 다시 실행하세요.", file=sys.stderr)
        sys.exit(1)

    port = int(port_s)
    msg = MIMEText(
        "Co-Talk SMTP 테스트 메일입니다.\n\n이 메일이 도착하면 SMTP 설정은 정상입니다.\n",
        "plain",
        "utf-8",
    )
    msg["Subject"] = "[Co-Talk] SMTP 테스트"
    msg["From"] = from_addr
    msg["To"] = args.to

    ctx = ssl.create_default_context()
    with smtplib.SMTP(host, port, timeout=30) as server:
        server.ehlo()
        server.starttls(context=ctx)
        server.ehlo()
        server.login(user, password)
        server.sendmail(from_addr, [args.to], msg.as_string())
    print(f"OK — sent test mail to {args.to}")


if __name__ == "__main__":
    main()
