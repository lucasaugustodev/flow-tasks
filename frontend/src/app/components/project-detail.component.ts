import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { ApiService } from '../services/api.service';
import { Project, Task, User, TaskStatus, TaskPriority } from '../models/task.model';

@Component({
  selector: 'app-project-detail',
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.scss']
})
export class ProjectDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  project: Project | null = null;
  projectTasks: Task[] = [];
  projectUsers: User[] = [];
  loading = false;
  activeTab: 'tasks' | 'access' | 'meetings' = 'tasks';
  
  // Meeting minutes data (mock for now)
  meetingMinutes = [
    {
      id: 1,
      title: 'Reunião de Kickoff',
      date: '2024-01-15',
      participants: ['João Silva', 'Maria Santos', 'Pedro Costa'],
      summary: 'Definição do escopo inicial do projeto e divisão de responsabilidades.',
      decisions: [
        'Tecnologia: Angular + Spring Boot',
        'Prazo: 3 meses',
        'Reuniões semanais às segundas-feiras'
      ],
      nextActions: [
        'Criar repositório Git',
        'Configurar ambiente de desenvolvimento',
        'Definir arquitetura inicial'
      ]
    },
    {
      id: 2,
      title: 'Review Sprint 1',
      date: '2024-01-22',
      participants: ['João Silva', 'Maria Santos'],
      summary: 'Revisão das funcionalidades desenvolvidas na primeira sprint.',
      decisions: [
        'Aprovado: Sistema de autenticação',
        'Pendente: Ajustes no design do dashboard'
      ],
      nextActions: [
        'Implementar correções no dashboard',
        'Iniciar desenvolvimento do módulo de projetos'
      ]
    }
  ];

  // Task statistics
  taskStats = {
    total: 0,
    completed: 0,
    inProgress: 0,
    pending: 0
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const projectId = +params['id'];
      if (projectId) {
        this.loadProjectDetails(projectId);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadProjectDetails(projectId: number): void {
    this.loading = true;
    
    forkJoin({
      project: this.apiService.getProject(projectId),
      tasks: this.apiService.getTasksByProject(projectId),
      users: this.apiService.getUsers()
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({ project, tasks, users }) => {
        this.project = project;
        this.projectTasks = tasks;
        this.projectUsers = users;
        this.calculateTaskStats();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading project details:', error);
        this.loading = false;

        if (error.status === 404) {
          alert('Projeto não encontrado.');
          this.router.navigate(['/projects']);
        } else if (error.status === 403) {
          alert('Você não tem acesso a este projeto.');
          this.router.navigate(['/projects']);
        } else {
          alert('Erro ao carregar detalhes do projeto: ' + (error.error?.message || error.message || 'Erro desconhecido'));
        }
      }
    });
  }

  private calculateTaskStats(): void {
    this.taskStats.total = this.projectTasks.length;
    this.taskStats.completed = this.projectTasks.filter(t => t.status === TaskStatus.DONE).length;
    this.taskStats.inProgress = this.projectTasks.filter(t => 
      t.status === TaskStatus.IN_PROGRESS || t.status === TaskStatus.IN_REVIEW
    ).length;
    this.taskStats.pending = this.projectTasks.filter(t => 
      t.status === TaskStatus.BACKLOG || t.status === TaskStatus.READY_TO_DEVELOP
    ).length;
  }

  setActiveTab(tab: 'tasks' | 'access' | 'meetings'): void {
    this.activeTab = tab;
  }

  goBack(): void {
    this.router.navigate(['/projects']);
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PLANNING':
        return '#6f42c1';
      case 'ACTIVE':
        return '#28A745';
      case 'ON_HOLD':
        return '#FFC107';
      case 'COMPLETED':
        return '#007AFF';
      case 'CANCELLED':
        return '#DC3545';
      default:
        return '#666666';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PLANNING':
        return 'Planejamento';
      case 'ACTIVE':
        return 'Ativo';
      case 'ON_HOLD':
        return 'Em Espera';
      case 'COMPLETED':
        return 'Concluído';
      case 'CANCELLED':
        return 'Cancelado';
      default:
        return status;
    }
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

  getTaskStatusLabel(status: TaskStatus): string {
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

  getCompletionPercentage(): number {
    if (this.taskStats.total === 0) return 0;
    return Math.round((this.taskStats.completed / this.taskStats.total) * 100);
  }

  getProjectDuration(): string {
    if (!this.project?.startDate || !this.project?.endDate) {
      return 'Duração não definida';
    }

    const start = new Date(this.project.startDate);
    const end = new Date(this.project.endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) {
      return '1 dia';
    } else if (diffDays < 30) {
      return `${diffDays} dias`;
    } else if (diffDays < 365) {
      const months = Math.round(diffDays / 30);
      return months === 1 ? '1 mês' : `${months} meses`;
    } else {
      const years = Math.round(diffDays / 365);
      return years === 1 ? '1 ano' : `${years} anos`;
    }
  }

  // Get users who have access to this project (users with tasks assigned)
  getProjectAccessUsers(): User[] {
    const userIds = new Set<number>();
    
    // Add project creator
    if (this.project?.createdBy) {
      userIds.add(this.project.createdBy.id);
    }
    
    // Add users with assigned tasks
    this.projectTasks.forEach(task => {
      if (task.assignedUser) {
        userIds.add(task.assignedUser.id);
      }
      if (task.createdBy) {
        userIds.add(task.createdBy.id);
      }
    });

    return this.projectUsers.filter(user => userIds.has(user.id));
  }
}
