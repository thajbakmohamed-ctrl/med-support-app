"""Disposable Docker SMTP probe; no application, DB, Kafka or secret files."""
import getpass
import smtplib
import ssl
import sys
from email.message import EmailMessage

if sys.argv[1:] not in (["connect"], ["send-one"]):
    raise SystemExit("Choose connect or send-one")

try:
    with smtplib.SMTP("smtp.gmail.com", 587, timeout=15) as smtp:
        smtp.ehlo()
        smtp.starttls(context=ssl.create_default_context())
        smtp.ehlo()
        print("PASS: Docker DNS, TCP 587, STARTTLS and certificate validation.")
        if sys.argv[1] == "send-one":
            username = input("Gmail sender address: ").strip()
            recipient = input("One recipient address you control: ").strip()
            for address in (username, recipient):
                if "@" not in address or any(c in address for c in "\r\n,;<>"):
                    raise ValueError("Use one plain email address")
            password = getpass.getpass("Gmail App Password (hidden): ").replace(" ", "")
            if input("Type SEND ONE to send exactly one test message: ") != "SEND ONE":
                raise SystemExit("Cancelled; no message sent.")
            smtp.login(username, password)
            password = None
            message = EmailMessage()
            message["From"] = username
            message["To"] = recipient
            message["Subject"] = "Med Support Project 4 controlled SMTP test"
            message.set_content("One manually requested SMTP test. No application workers were started.")
            smtp.send_message(message, from_addr=username, to_addrs=[recipient])
            print("SMTP accepted one message. Check the recipient inbox/spam; acceptance is not delivery confirmation.")
except Exception as error:
    # Do not echo server responses, exception text, addresses or credentials.
    print("FAIL: " + type(error).__name__ + "; details withheld to protect credentials.", file=sys.stderr)
    raise SystemExit(1)
