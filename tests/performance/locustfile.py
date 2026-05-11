import datetime
import os
import random
import uuid

from locust import HttpUser, between, task

AUTH_URL      = os.environ.get("AUTH_URL",      "http://localhost:8180")
GATEWAY_URL   = os.environ.get("GATEWAY_URL",   "http://localhost:8087")
PROMOTION_URL = os.environ.get("PROMOTION_URL", "http://localhost:8088")
FORM_URL      = os.environ.get("FORM_URL",      "http://localhost:8086")


class StudentUser(HttpUser):
    """Simulates a student doing daily campus check-in and health monitoring."""
    weight = 7
    wait_time = between(1, 3)
    host = AUTH_URL

    def on_start(self):
        resp = self.client.post(
            f"{AUTH_URL}/api/v1/auth/login",
            json={"username": "staff_guard", "password": "password"},
            name="POST /api/v1/auth/login [setup]",
        )
        if resp.status_code == 200:
            data = resp.json()
            self.token = data.get("token", "")
            self.anonymous_id = data.get("anonymousId", str(uuid.uuid4()))
            self.headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.token = ""
            self.anonymous_id = str(uuid.uuid4())
            self.headers = {}

    @task(3)
    def check_in(self):
        """Generate a QR token and validate it at the campus gate."""
        qr_resp = self.client.get(
            f"{AUTH_URL}/api/v1/auth/qr/generate",
            headers=self.headers,
            name="GET /api/v1/auth/qr/generate",
        )
        if qr_resp.status_code == 200:
            qr_token = qr_resp.json().get("qrToken", "invalid")
            self.client.post(
                f"{GATEWAY_URL}/api/v1/gate/validate",
                json={"token": qr_token},
                name="POST /api/v1/gate/validate",
            )

    @task(1)
    def get_health_status(self):
        """Check personal health status."""
        self.client.get(
            f"{PROMOTION_URL}/api/v1/promotion/status/{self.anonymous_id}",
            name="GET /api/v1/promotion/status/{id}",
        )

    @task(1)
    def submit_survey(self):
        """Submit a daily health survey."""
        self.client.post(
            f"{FORM_URL}/api/v1/surveys",
            json={
                "anonymousId": str(uuid.uuid4()),
                "hasFever": random.choice([True, False]),
                "hasCough": random.choice([True, False]),
                "exposureDate": datetime.date.today().isoformat(),
            },
            name="POST /api/v1/surveys",
        )


class HealthOfficerUser(HttpUser):
    """Simulates a health officer monitoring and reporting cases."""
    weight = 3
    wait_time = between(2, 5)
    host = AUTH_URL

    def on_start(self):
        resp = self.client.post(
            f"{AUTH_URL}/api/v1/auth/login",
            json={"username": "health_user", "password": "password"},
            name="POST /api/v1/auth/login [setup]",
        )
        if resp.status_code == 200:
            data = resp.json()
            self.token = data.get("token", "")
            self.anonymous_id = data.get("anonymousId", str(uuid.uuid4()))
            self.headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.token = ""
            self.anonymous_id = str(uuid.uuid4())
            self.headers = {}

    @task(1)
    def report_case(self):
        """Report a confirmed health case."""
        self.client.post(
            f"{PROMOTION_URL}/api/v1/promotion/report",
            json={"anonymousId": self.anonymous_id, "status": "CONFIRMED"},
            headers=self.headers,
            name="POST /api/v1/promotion/report",
        )

    @task(2)
    def view_stats(self):
        """View aggregated health statistics."""
        self.client.get(
            f"{PROMOTION_URL}/api/v1/health-status/stats",
            name="GET /api/v1/health-status/stats",
        )
