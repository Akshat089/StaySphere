import requests
import concurrent.futures
import time
from collections import Counter

BASE_URL = "http://localhost:8088"

GUEST_EMAIL = "dockerguest3@test.com"
GUEST_PASSWORD = "guest123"

PROPERTY_ID = 1

THREADS = 50

BOOKING_BODY = {
    "propertyId": PROPERTY_ID,
    "checkInDate": "2029-09-10",
    "checkOutDate": "2029-09-14",
    "totalAmount": 880,
    "currency": "EUR"
}


def login_guest():
    response = requests.post(
        f"{BASE_URL}/api/auth/login",
        json={
            "email": GUEST_EMAIL,
            "password": GUEST_PASSWORD
        },
        timeout=10
    )

    response.raise_for_status()
    return response.json()["token"]


def create_booking(token, request_id):
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(
            f"{BASE_URL}/api/bookings",
            json=BOOKING_BODY,
            headers=headers,
            timeout=20
        )

        return {
            "request_id": request_id,
            "status_code": response.status_code,
            "success": 200 <= response.status_code < 300,
            "body": response.text
        }

    except Exception as e:
        return {
            "request_id": request_id,
            "status_code": "EXCEPTION",
            "success": False,
            "body": str(e)
        }


def main():
    print("Logging in guest...")
    token = login_guest()
    print("Guest login successful.")

    print(f"Starting benchmark with {THREADS} concurrent requests...")
    start = time.time()

    results = []

    with concurrent.futures.ThreadPoolExecutor(max_workers=THREADS) as executor:
        futures = [
            executor.submit(create_booking, token, i)
            for i in range(THREADS)
        ]

        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    end = time.time()

    success_count = sum(1 for r in results if r["success"])
    failure_count = THREADS - success_count

    status_counts = Counter(str(r["status_code"]) for r in results)

    print("\n========== BENCHMARK RESULT ==========")
    print(f"Total requests: {THREADS}")
    print(f"Successful bookings: {success_count}")
    print(f"Failed bookings: {failure_count}")
    print(f"Total time: {end - start:.2f} seconds")
    print(f"Status counts: {dict(status_counts)}")

    print("\n========== SAMPLE FAILURES ==========")
    failures = [r for r in results if not r["success"]]

    for failure in failures[:5]:
        print(f"\nRequest ID: {failure['request_id']}")
        print(f"Status: {failure['status_code']}")
        print(f"Body: {failure['body'][:500]}")

    print("\n========== PASS CRITERIA ==========")

    if success_count == 1:
        print("PASS: Exactly 1 booking succeeded.")
    else:
        print("FAIL: Expected exactly 1 successful booking.")

    if failure_count == THREADS - 1:
        print("PASS: Remaining requests failed safely.")
    else:
        print("FAIL: Unexpected failure/success count.")


if __name__ == "__main__":
    main()