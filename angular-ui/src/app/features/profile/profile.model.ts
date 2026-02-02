export interface UserProfile {
  id?: number;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  gender: 'MALE' | 'FEMALE';
  age: number;
}

export type Gender = 'MALE' | 'FEMALE';
