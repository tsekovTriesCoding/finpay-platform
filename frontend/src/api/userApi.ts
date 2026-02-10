import api from './axios';
import type { User } from './authApi';

export interface UpdateProfileRequest {
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string | null;
  profileImageUrl?: string | null;
  address?: string | null;
  city?: string | null;
  country?: string | null;
  postalCode?: string | null;
}

export interface ProfileImageResponse {
  profileImageUrl: string;
}

export const userService = {
  getProfile: (userId: string) =>
    api.get<User>(`/api/v1/users/${userId}`).then(r => r.data),

  updateProfile: (userId: string, data: UpdateProfileRequest) =>
    api.put<User>(`/api/v1/users/${userId}`, data).then(r => r.data),

  uploadProfileImage: (userId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api
      .post<ProfileImageResponse>(`/api/v1/users/${userId}/profile-image`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then(r => r.data);
  },

  deleteProfileImage: (userId: string) =>
    api.delete<void>(`/api/v1/users/${userId}/profile-image`),
};
