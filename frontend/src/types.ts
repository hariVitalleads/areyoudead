export interface UserResponse {
  id: string;
  email: string;
  createdAt: string;
}

export interface AuthResponse {
  tokenType: string;
  accessToken: string;
  user: UserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  registrationType?: string;
  firstName?: string;
  middleName?: string;
  lastName?: string;
  country?: string;
  state?: string;
  mobileNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
}

export interface AppUserDetailsResponse {
  id: string;
  email: string;
  createdAt: string;
  lastLoginDate: string | null;
}

export interface AccountDetailsResponse {
  id: string;
  email: string;
  createdAt: string;
  lastLoginDate: string | null;
  registrationType?: string;
  firstName?: string;
  middleName?: string;
  lastName?: string;
  country?: string;
  state?: string;
  mobileNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  hasPaid?: boolean;
  paidAt?: string | null;
}

export interface UpdateAppUserRequest {
  email?: string;
}

export interface UpdateDetailsRequest {
  email?: string;
  registrationType?: string;
  firstName?: string;
  middleName?: string;
  lastName?: string;
  country?: string;
  state?: string;
  mobileNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordResponse {
  message: string;
  resetToken: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface MessageResponse {
  message: string;
}

export interface EmergencyContactRequest {
  mobileNumber: string;
  email: string;
}

export interface EmergencyContactResponse {
  id: string;
  mobileNumber: string;
  email: string;
  contactIndex: number;
}
