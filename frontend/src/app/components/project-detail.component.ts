import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { ApiService } from '../services/api.service';
import { Project, Task, User, TaskStatus, TaskPriority, CreateTaskRequest, TaskColumn } from '../models/task.model';

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
  allUsers: User[] = [];
  taskColumns: TaskColumn[] = [];
  loading = false;
  activeTab: 'tasks' | 'access' | 'meetings' = 'tasks';

  // Modal states
  isCreateTaskModalOpen = false;
  isCreateMeetingModalOpen = false;
  isAddUserModalOpen = false;
  selectedTask: Task | null = null;

  // Forms
  newTask: CreateTaskRequest = {
    title: '',
    description: '',
    priority: TaskPriority.MEDIUM,
    status: 'Backlog',
    projectId: 0,
    assignedUserId: null,
    dueDate: ''
  };

  newMeeting = {
    title: '',
    summary: '',
    meetingDate: '',
    participants: [] as string[],
    decisions: [] as string[],
    nextActions: [] as string[]
  };

  selectedUsersToAdd: number[] = [];
  
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
      users: this.apiService.getUsers(),
      columns: this.apiService.getTaskColumns(projectId)
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({ project, tasks, users, columns }) => {
        this.project = project;
        this.projectTasks = tasks;
        this.allUsers = users;
        this.projectUsers = users;
        this.taskColumns = columns;
        this.calculateTaskStats();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading project details:', error);
        this.loading = false;

        // Set default columns if API fails
        this.taskColumns = this.getDefaultColumns();

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
    this.taskStats.completed = this.projectTasks.filter(t => t.status === 'Concluído').length;
    this.taskStats.inProgress = this.projectTasks.filter(t =>
      t.status === 'Em Progresso' || t.status === 'Em Revisão'
    ).length;
    this.taskStats.pending = this.projectTasks.filter(t =>
      t.status === 'Backlog' || t.status === 'A Fazer'
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

  getTaskStatusLabel(status: string): string {
    return status; // Now status is already the display name
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

  // Task Management
  openCreateTaskModal(): void {
    this.newTask = {
      title: '',
      description: '',
      priority: TaskPriority.MEDIUM,
      status: this.taskColumns.length > 0 ? this.taskColumns[0].name : 'Backlog',
      projectId: this.project?.id || 0,
      assignedUserId: null,
      dueDate: ''
    };
    this.isCreateTaskModalOpen = true;
  }

  closeCreateTaskModal(): void {
    this.isCreateTaskModalOpen = false;
  }

  createTask(): void {
    if (!this.newTask.title.trim()) {
      return;
    }

    this.newTask.projectId = this.project?.id || 0;

    this.apiService.createTask(this.newTask)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (task) => {
          this.projectTasks.push(task);
          this.calculateTaskStats();
          this.closeCreateTaskModal();
        },
        error: (error) => {
          console.error('Error creating task:', error);
          alert('Erro ao criar tarefa: ' + (error.error?.message || error.message));
        }
      });
  }

  deleteTask(taskId: number): void {
    if (!confirm('Tem certeza que deseja excluir esta tarefa?')) {
      return;
    }

    this.apiService.deleteTask(taskId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.projectTasks = this.projectTasks.filter(t => t.id !== taskId);
          this.calculateTaskStats();
        },
        error: (error) => {
          console.error('Error deleting task:', error);
          alert('Erro ao excluir tarefa: ' + (error.error?.message || error.message));
        }
      });
  }

  // Meeting Management
  openCreateMeetingModal(): void {
    this.newMeeting = {
      title: '',
      summary: '',
      meetingDate: '',
      participants: [],
      decisions: [],
      nextActions: []
    };
    this.isCreateMeetingModalOpen = true;
  }

  closeCreateMeetingModal(): void {
    this.isCreateMeetingModalOpen = false;
  }

  createMeeting(): void {
    if (!this.newMeeting.title.trim()) {
      return;
    }

    // TODO: Implement meeting creation API call
    console.log('Creating meeting:', this.newMeeting);
    alert('Funcionalidade de atas de reunião será implementada em breve!');
    this.closeCreateMeetingModal();
  }

  // User Access Management
  openAddUserModal(): void {
    this.selectedUsersToAdd = [];
    this.isAddUserModalOpen = true;
  }

  closeAddUserModal(): void {
    this.isAddUserModalOpen = false;
  }

  addUsersToProject(): void {
    if (this.selectedUsersToAdd.length === 0) {
      return;
    }

    // TODO: Implement add users to project API call
    console.log('Adding users to project:', this.selectedUsersToAdd);
    alert('Funcionalidade de adicionar usuários será implementada em breve!');
    this.closeAddUserModal();
  }

  toggleUserSelection(userId: number): void {
    const index = this.selectedUsersToAdd.indexOf(userId);
    if (index > -1) {
      this.selectedUsersToAdd.splice(index, 1);
    } else {
      this.selectedUsersToAdd.push(userId);
    }
  }

  isUserSelected(userId: number): boolean {
    return this.selectedUsersToAdd.includes(userId);
  }

  getUsersNotInProject(): User[] {
    const projectUserIds = this.getProjectAccessUsers().map(u => u.id);
    return this.allUsers.filter(user => !projectUserIds.includes(user.id));
  }

  // Task Detail Modal
  openTaskDetail(task: Task): void {
    this.selectedTask = task;
  }

  closeTaskDetail(): void {
    this.selectedTask = null;
  }

  onTaskUpdated(updatedTask: Task): void {
    const index = this.projectTasks.findIndex(t => t.id === updatedTask.id);
    if (index > -1) {
      this.projectTasks[index] = updatedTask;
      this.calculateTaskStats();
    }
    // Reload the task details to get updated data
    if (this.project?.id) {
      this.loadProjectDetails(this.project.id);
    }
  }

  onTaskDeleted(taskId: number): void {
    this.projectTasks = this.projectTasks.filter(t => t.id !== taskId);
    this.calculateTaskStats();
    this.selectedTask = null;
  }

  // Default columns fallback
  private getDefaultColumns(): TaskColumn[] {
    return [
      {
        id: 1,
        name: 'Backlog',
        description: 'Tarefas em espera',
        order: 1,
        color: '#6B7280',
        isDefault: true,
        projectId: this.project?.id || 0,
        projectName: this.project?.name || '',
        createdById: this.project?.createdBy?.id || 0,
        createdByName: this.project?.createdBy?.fullName || ''
      },
      {
        id: 2,
        name: 'A Fazer',
        description: 'Tarefas prontas para desenvolvimento',
        order: 2,
        color: '#3B82F6',
        isDefault: true,
        projectId: this.project?.id || 0,
        projectName: this.project?.name || '',
        createdById: this.project?.createdBy?.id || 0,
        createdByName: this.project?.createdBy?.fullName || ''
      },
      {
        id: 3,
        name: 'Em Progresso',
        description: 'Tarefas sendo desenvolvidas',
        order: 3,
        color: '#F59E0B',
        isDefault: true,
        projectId: this.project?.id || 0,
        projectName: this.project?.name || '',
        createdById: this.project?.createdBy?.id || 0,
        createdByName: this.project?.createdBy?.fullName || ''
      },
      {
        id: 4,
        name: 'Em Revisão',
        description: 'Tarefas em revisão',
        order: 4,
        color: '#8B5CF6',
        isDefault: true,
        projectId: this.project?.id || 0,
        projectName: this.project?.name || '',
        createdById: this.project?.createdBy?.id || 0,
        createdByName: this.project?.createdBy?.fullName || ''
      },
      {
        id: 5,
        name: 'Concluído',
        description: 'Tarefas finalizadas',
        order: 5,
        color: '#10B981',
        isDefault: true,
        projectId: this.project?.id || 0,
        projectName: this.project?.name || '',
        createdById: this.project?.createdBy?.id || 0,
        createdByName: this.project?.createdBy?.fullName || ''
      }
    ];
  }
}
