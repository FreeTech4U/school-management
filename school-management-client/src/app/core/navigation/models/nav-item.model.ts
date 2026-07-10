import { MockAuthRole } from '../../auth/models/auth.models';

export interface NavItem {
  key: string;
  labelKey: string;
  route: string;
  allowedRoles?: MockAuthRole[];
  children?: NavItem[];
}

