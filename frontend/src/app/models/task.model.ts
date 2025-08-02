export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  isActive: boolean;
  createdAt: string;
}

export interface Project {
  id: number;
  name: string;
  description: string;
  status: ProjectStatus;
  startDate?: string;
  endDate?: string;
  createdAt: string;
  updatedAt: string;
  createdBy: User;
}

export interface Task {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
  project: Project;
  assignedUser?: User;
  createdBy: User;
  comments?: TaskComment[];
  checklistItems?: TaskChecklistItem[];
}

export interface TaskComment {
  id: number;
  content: string;
  createdAt: string;
  updatedAt: string;
  createdBy: User;
}

export interface TaskChecklistItem {
  id: number;
  description: string;
  isCompleted: boolean;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  createdBy: User;
  completedBy?: User;
}

export enum TaskStatus {
  BACKLOG = 'BACKLOG',
  READY_TO_DEVELOP = 'READY_TO_DEVELOP',
  IN_PROGRESS = 'IN_PROGRESS',
  IN_REVIEW = 'IN_REVIEW',
  DONE = 'DONE'
}

export enum TaskPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export enum ProjectStatus {
  PLANNING = 'PLANNING',
  ACTIVE = 'ACTIVE',
  ON_HOLD = 'ON_HOLD',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  priority: TaskPriority;
  dueDate?: string;
  project: { id: number };
  assignedUserId?: number;
}

export interface UpdateTaskStatusRequest {
  status: TaskStatus;
}

export interface CreateCommentRequest {
  content: string;
}

export interface CreateChecklistItemRequest {
  description: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

// Kanban Column Configuration
export interface KanbanColumn {
  id: TaskStatus;
  title: string;
  icon: string;
  color: string;
  tasks: Task[];
}

// Drag and Drop Interfaces
export interface DragDropData {
  taskId: number;
  sourceColumnId: TaskStatus;
  targetColumnId: TaskStatus;
  sourceIndex: number;
  targetIndex: number;
}
