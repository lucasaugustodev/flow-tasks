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
    this.loadCurrentUser();
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    });
  }

  private loadCurrentUser(): void {
    const token = localStorage.getItem('token');
    if (token) {
      this.getCurrentUser().subscribe({
        next: (user) => this.currentUserSubject.next(user),
        error: () => this.logout()
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
    localStorage.setItem('token', token);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  // Users
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`, { headers: this.getAuthHeaders() });
  }

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/me`, { headers: this.getAuthHeaders() });
  }

  // Projects
  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.baseUrl}/projects`, { headers: this.getAuthHeaders() });
  }

  getProject(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/projects/${id}`, { headers: this.getAuthHeaders() });
  }

  createProject(project: Partial<Project>): Observable<Project> {
    return this.http.post<Project>(`${this.baseUrl}/projects`, project, { headers: this.getAuthHeaders() });
  }

  // Tasks
  getTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/tasks`, { headers: this.getAuthHeaders() });
  }

  getTasksByProject(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/projects/${projectId}/tasks`, { headers: this.getAuthHeaders() });
  }

  getTask(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.baseUrl}/tasks/${id}`, { headers: this.getAuthHeaders() });
  }

  createTask(task: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(`${this.baseUrl}/tasks`, task, { headers: this.getAuthHeaders() });
  }

  updateTaskStatus(taskId: number, status: UpdateTaskStatusRequest): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/tasks/${taskId}/status`, status, { headers: this.getAuthHeaders() });
  }

  assignTask(taskId: number, assignedUserId: number | null): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/tasks/${taskId}/assign`, 
      { assignedUserId }, 
      { headers: this.getAuthHeaders() }
    );
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tasks/${id}`, { headers: this.getAuthHeaders() });
  }

  // Task Comments
  getTaskComments(taskId: number): Observable<TaskComment[]> {
    return this.http.get<TaskComment[]>(`${this.baseUrl}/tasks/${taskId}/comments`, { headers: this.getAuthHeaders() });
  }

  createTaskComment(taskId: number, comment: CreateCommentRequest): Observable<TaskComment> {
    return this.http.post<TaskComment>(`${this.baseUrl}/tasks/${taskId}/comments`, comment, { headers: this.getAuthHeaders() });
  }

  updateTaskComment(taskId: number, commentId: number, comment: CreateCommentRequest): Observable<TaskComment> {
    return this.http.put<TaskComment>(`${this.baseUrl}/tasks/${taskId}/comments/${commentId}`, comment, { headers: this.getAuthHeaders() });
  }

  deleteTaskComment(taskId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tasks/${taskId}/comments/${commentId}`, { headers: this.getAuthHeaders() });
  }

  // Task Checklist
  getTaskChecklist(taskId: number): Observable<TaskChecklistItem[]> {
    return this.http.get<TaskChecklistItem[]>(`${this.baseUrl}/tasks/${taskId}/checklist`, { headers: this.getAuthHeaders() });
  }

  createChecklistItem(taskId: number, item: CreateChecklistItemRequest): Observable<TaskChecklistItem> {
    return this.http.post<TaskChecklistItem>(`${this.baseUrl}/tasks/${taskId}/checklist`, item, { headers: this.getAuthHeaders() });
  }

  updateChecklistItem(taskId: number, itemId: number, item: CreateChecklistItemRequest): Observable<TaskChecklistItem> {
    return this.http.put<TaskChecklistItem>(`${this.baseUrl}/tasks/${taskId}/checklist/${itemId}`, item, { headers: this.getAuthHeaders() });
  }

  toggleChecklistItem(taskId: number, itemId: number): Observable<TaskChecklistItem> {
    return this.http.put<TaskChecklistItem>(`${this.baseUrl}/tasks/${taskId}/checklist/${itemId}/toggle`, {}, { headers: this.getAuthHeaders() });
  }

  deleteChecklistItem(taskId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tasks/${taskId}/checklist/${itemId}`, { headers: this.getAuthHeaders() });
  }

  getChecklistStats(taskId: number): Observable<{total: number, completed: number, remaining: number}> {
    return this.http.get<{total: number, completed: number, remaining: number}>(`${this.baseUrl}/tasks/${taskId}/checklist/stats`, { headers: this.getAuthHeaders() });
  }
}
