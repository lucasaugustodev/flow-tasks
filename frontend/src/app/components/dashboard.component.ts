import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { ApiService } from '../services/api.service';
import { Task, Project, User, TaskStatus, TaskPriority } from '../models/task.model';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  currentUser: User | null = null;
  projects: Project[] = [];
  tasks: Task[] = [];
  loading = false;

  // Statistics
  stats = {
    totalProjects: 0,
    activeProjects: 0,
    totalTasks: 0,
    completedTasks: 0,
    myTasks: 0,
    overdueTasks: 0
  };

  // Recent activities
  recentTasks: Task[] = [];
  myTasks: Task[] = [];

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadCurrentUser();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCurrentUser(): void {
    this.apiService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
      });
  }

  loadDashboardData(): void {
    this.loading = true;
    
    forkJoin({
      projects: this.apiService.getProjects(),
      tasks: this.apiService.getTasks()
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({ projects, tasks }) => {
        this.projects = projects;
        this.tasks = tasks;
        this.calculateStats();
        this.organizeRecentData();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
        this.loading = false;
      }
    });
  }

  private calculateStats(): void {
    this.stats.totalProjects = this.projects.length;
    this.stats.activeProjects = this.projects.filter(p => p.status === 'ACTIVE').length;
    this.stats.totalTasks = this.tasks.length;
    this.stats.completedTasks = this.tasks.filter(t => t.status === TaskStatus.DONE).length;
    
    if (this.currentUser) {
      this.stats.myTasks = this.tasks.filter(t => 
        t.assignedUser?.id === this.currentUser?.id
      ).length;
    }
    
    // Calculate overdue tasks
    const now = new Date();
    this.stats.overdueTasks = this.tasks.filter(t => 
      t.dueDate && 
      new Date(t.dueDate) < now && 
      t.status !== TaskStatus.DONE
    ).length;
  }

  private organizeRecentData(): void {
    // Recent tasks (last 10 created)
    this.recentTasks = [...this.tasks]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);

    // My tasks (assigned to current user)
    if (this.currentUser) {
      this.myTasks = this.tasks
        .filter(t => t.assignedUser?.id === this.currentUser?.id)
        .filter(t => t.status !== TaskStatus.DONE)
        .sort((a, b) => {
          // Sort by priority and due date
          const priorityOrder = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
          const aPriority = priorityOrder[a.priority] || 0;
          const bPriority = priorityOrder[b.priority] || 0;
          
          if (aPriority !== bPriority) {
            return bPriority - aPriority;
          }
          
          if (a.dueDate && b.dueDate) {
            return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
          }
          
          return 0;
        })
        .slice(0, 5);
    }
  }

  getCompletionPercentage(): number {
    if (this.stats.totalTasks === 0) return 0;
    return Math.round((this.stats.completedTasks / this.stats.totalTasks) * 100);
  }

  getPriorityColor(priority: TaskPriority): string {
    switch (priority) {
      case TaskPriority.LOW:
        return '#28A745';
      case TaskPriority.MEDIUM:
        return '#FFC107';
      case TaskPriority.HIGH:
        return '#DC3545';
      case TaskPriority.URGENT:
        return '#6f42c1';
      default:
        return '#666666';
    }
  }

  getPriorityLabel(priority: TaskPriority): string {
    switch (priority) {
      case TaskPriority.LOW:
        return 'Baixa';
      case TaskPriority.MEDIUM:
        return 'Média';
      case TaskPriority.HIGH:
        return 'Alta';
      case TaskPriority.URGENT:
        return 'Urgente';
      default:
        return 'Média';
    }
  }

  getStatusLabel(status: TaskStatus): string {
    switch (status) {
      case TaskStatus.BACKLOG:
        return 'Backlog';
      case TaskStatus.READY_TO_DEVELOP:
        return 'Pronto';
      case TaskStatus.IN_PROGRESS:
        return 'Em Progresso';
      case TaskStatus.IN_REVIEW:
        return 'Em Revisão';
      case TaskStatus.DONE:
        return 'Concluído';
      default:
        return status;
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('pt-BR');
  }

  isOverdue(task: Task): boolean {
    if (!task.dueDate) return false;
    return new Date(task.dueDate) < new Date() && task.status !== TaskStatus.DONE;
  }
}
