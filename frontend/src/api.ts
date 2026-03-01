import type {
  RegisterRequest,
  LoginRequest,
  UserResponse,
  AuthResponse,
  ForgotPasswordResponse,
  ResetPasswordRequest,
  MessageResponse,
  AccountDetailsResponse,
  UpdateDetailsRequest,
  EmergencyContactRequest,
  EmergencyContactResponse,
  CheckInSummaryResponse,
  AuditEventResponse,
} from './types';

const API_BASE =
  import.meta.env.DEV
    ? '/api'
    : (import.meta.env.VITE_API_URL || '') + '/api';

function getToken(): string | null {
  return localStorage.getItem('token');
}

function headers(includeAuth = true): HeadersInit {
  const h: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  const token = getToken();
  if (includeAuth && token) {
    h['Authorization'] = `Bearer ${token}`;
  }
  return h;
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text();
    let msg = text;
    try {
      const json = JSON.parse(text);
      msg = json.message ?? json.error ?? text;
    } catch {
      msg = text || res.statusText;
    }
    throw new Error(msg);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export async function register(body: RegisterRequest): Promise<UserResponse> {
  return fetch(`${API_BASE}/user/register`, {
    method: 'POST',
    headers: headers(false),
    body: JSON.stringify(body),
  }).then(handleResponse<UserResponse>);
}

export async function login(body: LoginRequest): Promise<AuthResponse> {
  return fetch(`${API_BASE}/user/login`, {
    method: 'POST',
    headers: headers(false),
    body: JSON.stringify(body),
  }).then(handleResponse<AuthResponse>);
}

export async function forgotPassword(
  email: string
): Promise<ForgotPasswordResponse> {
  return fetch(`${API_BASE}/user/forgot-password`, {
    method: 'POST',
    headers: headers(false),
    body: JSON.stringify({ email }),
  }).then(handleResponse<ForgotPasswordResponse>);
}

export async function resetPassword(
  body: ResetPasswordRequest
): Promise<MessageResponse> {
  return fetch(`${API_BASE}/user/reset-password`, {
    method: 'POST',
    headers: headers(false),
    body: JSON.stringify(body),
  }).then(handleResponse<MessageResponse>);
}

export async function getAccount(): Promise<AccountDetailsResponse> {
  return fetch(`${API_BASE}/user/me`, { headers: headers() }).then(
    handleResponse<AccountDetailsResponse>
  );
}

export async function updateAccountDetails(
  body: UpdateDetailsRequest
): Promise<AccountDetailsResponse> {
  return fetch(`${API_BASE}/user/details`, {
    method: 'PUT',
    headers: headers(),
    body: JSON.stringify(body),
  }).then(handleResponse<AccountDetailsResponse>);
}

export async function getEmergencyContacts(): Promise<
  EmergencyContactResponse[]
> {
  return fetch(`${API_BASE}/emergency-contacts`, { headers: headers() }).then(
    handleResponse<EmergencyContactResponse[]>
  );
}

export async function addEmergencyContact(
  body: EmergencyContactRequest
): Promise<EmergencyContactResponse> {
  return fetch(`${API_BASE}/emergency-contacts`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify(body),
  }).then(handleResponse<EmergencyContactResponse>);
}

export async function updateEmergencyContact(
  id: string,
  body: EmergencyContactRequest
): Promise<EmergencyContactResponse> {
  return fetch(`${API_BASE}/emergency-contacts/${id}`, {
    method: 'PUT',
    headers: headers(),
    body: JSON.stringify(body),
  }).then(handleResponse<EmergencyContactResponse>);
}

export async function deleteEmergencyContact(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/emergency-contacts/${id}`, {
    method: 'DELETE',
    headers: headers(),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
}

export async function getCheckInSummary(): Promise<CheckInSummaryResponse> {
  return fetch(`${API_BASE}/user/check-in-summary`, { headers: headers() }).then(
    handleResponse<CheckInSummaryResponse>
  );
}

export async function getAuditEvents(): Promise<AuditEventResponse[]> {
  return fetch(`${API_BASE}/user/audit-events`, { headers: headers() }).then(
    handleResponse<AuditEventResponse[]>
  );
}

export async function checkIn(snoozeDays?: number): Promise<MessageResponse> {
  return fetch(`${API_BASE}/user/check-in`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify(snoozeDays != null ? { snoozeDays } : {}),
  }).then(handleResponse<MessageResponse>);
}
