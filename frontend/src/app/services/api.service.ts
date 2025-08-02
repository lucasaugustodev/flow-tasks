import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { 
  Task, 
  Project, 
  User, 
  TaskComment, 
  TaskChecklistItem,
  CreateTaskRequest,
  UpdateTaskStatusRequest,
  CreateCommentRequest,
  CreateChecklistItemRequest,
  LoginRequest,
  LoginResponse
} from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = 'http://localhost:8080/api';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    // Don't load user automatically - let components handle it
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Getting auth headers with token:', token ? 'Token exists' : 'No token');
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    });
  }



  private loadCurrentUser(): void {
    const token = localStorage.getItem('token');
    console.log('Loading current user with token:', !!token);
    if (token) {
      this.getCurrentUser().subscribe({
        next: (user) => {
          console.log('Current user loaded:', user);
          this.currentUserSubject.next(user);
        },
        error: (error) => {
          console.error('Error loading current user:', error);
          // Only logout on 401, not on network errors
          if (error.status === 401) {
            console.log('Token invalid - logging out from loadCurrentUser');
            this.logout();
          }
        }
      });
    }
  }

  // Authentication
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/signin`, credentials);
  }

  logout(): void {
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
  }

  setToken(token: string): void {
    console.log('Setting token:', token ? 'Token provided' : 'No token');
    localStorage.setItem('token', token);
    // Load current user after setting token
    this.loadCurrentUser();
  }

  isLoggedIn(): boolean {
    const token = localStorage.getItem('token');
    console.log('Checking if logged in - token exists:', !!token);
    return !!token;
  }

  // Users
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`);
  }

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/me`);
  }

  // Projects
  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.baseUrl}/projects`);
  }

  getProject(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/projects/${id}`);
  }

  createProject(project: Partial<Project>): Observable<Project> {
    return this.http.post<Project>(`${this.baseUrl}/projects`, project);
  }

  // Tasks
  getTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/tasks`);
  }

  getTasksByProject(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/projects/${projectId}/tasks`);
  }

  getTask(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.baseUrl}/tasks/${id}`);
  }

  createTask(taskRequest: CreateTaskRequest): Observable<Task> {
    // Convert CreateTaskRequest to the format expected by backend
    const taskData = {
      title: taskRequest.title,
      description: taskRequest.description || '',
      priority: taskRequest.priority,
      dueDate: taskRequest.dueDate ? new Date(taskRequest.dueDate + 'T23:59:59').toISOString() : null,
      project: { id: taskRequest.project.id },
      assignedUser: taskRequest.assignedUserId ? { id: taskRequest.assignedUserId } : null
    };

    console.log('Sending task data to backend:', taskData);
    return this.http.post<Task>(`${this.baseUrl}/tasks`, taskData);
  }

  updateTaskStatus(taskId: number, status: UpdateTaskStatusRequest): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/tasks/${taskId}/status?status=${status.status}`, {});
  }

  assignTask(taskId: number, assignedUserId: number | null): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/tasks/${taskId}/assign`, { assignedUserId });
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tasks/${id}`);
  }

  // Task Comments
  getTaskComments(taskId: number): Observable<TaskComment[]> {
    return this.http.get<TaskComment[]>(`${this.baseUrl}/tasks/${taskId}/comments`);
  }

  createTaskComment(taskId: number, comment: CreateCommentRequest): Observable<TaskComment> {
    console.log('Creating comment for task', taskId, 'with data:', comment);
    return this.http.post<TaskComment>(`${this.baseUrl}/tasks/${taskId}/comments`, comment);
  }

  updateTaskComment(taskId: number, commentId: number, comment: CreateCommentRequest): Observable<TaskComment> {
    return this.http.put<TaskComment>(`${this.baseUrl}/tasks/${taskId}/comments/${commentId}`, comment, { headers: this.getAuthHeaders() });
  }

  deleteTaskComment(taskId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tasks/${taskId}/comments/${commentId}`, { headers: this.getAuthHeaders() });
  }

  // Task Checklist
  getTaskChecklist(taskId: number): Observable<TaskChecklistItem[]> {
    return this.http.get<TaskChecklistItem[]>(`${this.baseUrl}/tasks/${taskId}/checklist`);
  }

  createChecklistItem(taskId: number, item: CreateChecklistItemRequest): Observable<TaskChecklistItem> {
    console.log('Creating checklist item for task', taskId, 'with data:', item);
    return this.http.post<TaskChecklistItem>(`${this.baseUrl}/tasks/${taskId}/checklist`, item);
  }

  updateChecklistItem(taskId: number, itemId: number, item: CreateChecklistItemRequest): Observable<TaskChecklistItem> {
    return this.http.put<TaskChecklistItem>(`${this.baseUrl}/tasks/${taskId}/checklist/${itemId}`, item, { headers: this.getAuthHeaders() });
  }

  toggleChecklistItem(taskId: number, itemId: number): Observable<TaskChecklistItem> {
    return this.http.put<TaskChecklistItem>(`${this.baseUrl}/tasks/${taskId}/checklist/${itemId}/toggle`, {});
  }

  deleteChecklistItem(taskId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tasks/${taskId}/checklist/${itemId}`);
  }

  getChecklistStats(taskId: number): Observable<{total: number, completed: number, remaining: number}> {
    return this.http.get<{total: number, completed: number, remaining: number}>(`${this.baseUrl}/tasks/${taskId}/checklist/stats`, { headers: this.getAuthHeaders() });
  }
}
