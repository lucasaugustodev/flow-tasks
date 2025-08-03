import { Component, OnInit, OnDestroy } from '@angular/core';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Subject, takeUntil, forkJoin } from 'rxjs';
import { ApiService } from '../services/api.service';
import { 
  Task, 
  TaskStatus, 
  TaskPriority, 
  KanbanColumn, 
  User, 
  Project,
  CreateTaskRequest 
} from '../models/task.model';

@Component({
  selector: 'app-kanban-board',
  templateUrl: './kanban-board.component.html',
  styleUrls: ['./kanban-board.component.scss']
})
export class KanbanBoardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Filter properties
  projects: Project[] = [];
  selectedProjectId: number | string = '';
  allTasks: Task[] = [];
  filteredTasks: Task[] = [];
  
  columns: KanbanColumn[] = [
    {
      id: 'BACKLOG',
      title: 'Backlog',
      icon: 'inbox',
      color: '#666666',
      tasks: []
    },
    {
      id: 'READY_TO_DEVELOP',
      title: 'A Fazer',
      icon: 'play',
      color: '#007AFF',
      tasks: []
    },
    {
      id: 'IN_PROGRESS',
      title: 'Em Progresso',
      icon: 'spinner',
      color: '#FFC107',
      tasks: []
    },
    {
      id: 'IN_REVIEW',
      title: 'Em Revisão',
      icon: 'eye',
      color: '#6f42c1',
      tasks: []
    },
    {
      id: 'DONE',
      title: 'Concluído',
      icon: 'check',
      color: '#28A745',
      tasks: []
    }
  ];

  users: User[] = [];
  selectedTask: Task | null = null;
  isCreateTaskModalOpen = false;
  isTaskDetailModalOpen = false;
  loading = false;

  // Create Task Form
  newTask: CreateTaskRequest = {
    title: '',
    description: '',
    priority: TaskPriority.MEDIUM,
    status: TaskStatus.BACKLOG,
    projectId: 0,
    assignedUserId: null
  };

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadData(): void {
    this.loading = true;
    console.log('Loading kanban data...');

    forkJoin({
      tasks: this.apiService.getTasks(),
      users: this.apiService.getUsers(),
      projects: this.apiService.getProjects()
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({ tasks, users, projects }) => {
        console.log('Data loaded successfully:', {
          tasksCount: tasks.length,
          usersCount: users.length,
          projectsCount: projects.length
        });

        console.log('=== DETAILED TASKS DEBUG ===');
        console.log('Raw tasks received:', tasks);
        tasks.forEach((task, index) => {
          console.log(`Task ${index + 1}:`, {
            id: task.id,
            title: task.title,
            status: task.status,
            project: task.project ? { id: task.project.id, name: task.project.name } : 'NO PROJECT'
          });
        });
        console.log('=== END DETAILED TASKS DEBUG ===');

        this.allTasks = tasks;
        this.users = users;
        this.projects = projects;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.loading = false;
        alert('Erro ao carregar dados: ' + (error.error?.message || error.message || 'Erro desconhecido'));
      }
    });
  }

  applyFilters(): void {
    console.log('=== APPLY FILTERS DEBUG ===');
    console.log('selectedProjectId:', this.selectedProjectId, 'type:', typeof this.selectedProjectId);
    console.log('allTasks count:', this.allTasks.length);

    // Filter tasks by selected project
    if (this.selectedProjectId && this.selectedProjectId !== '') {
      // Convert selectedProjectId to number for comparison
      const projectId = Number(this.selectedProjectId);
      console.log('Filtering for projectId:', projectId);

      this.filteredTasks = this.allTasks.filter(task => {
        const matches = task.project && task.project.id === projectId;
        console.log(`Task "${task.title}" (project: ${task.project?.id}) matches: ${matches}`);
        return matches;
      });

      console.log('Filtered tasks count:', this.filteredTasks.length);
    } else {
      this.filteredTasks = [...this.allTasks];
      console.log('Using all tasks:', this.filteredTasks.length);
    }

    console.log('Tasks going to organizeTasks:', this.filteredTasks.length);
    console.log('=== END APPLY FILTERS DEBUG ===');
    this.organizeTasks(this.filteredTasks);
  }

  onProjectFilterChange(): void {
    this.applyFilters();
  }

  clearProjectFilter(): void {
    this.selectedProjectId = '';
    this.applyFilters();
  }

  private organizeTasks(tasks: Task[]): void {
    console.log('=== ORGANIZE TASKS DEBUG ===');
    console.log('Tasks to organize:', tasks.length);
    console.log('Available columns:', this.columns.map(c => ({ id: c.id, title: c.title })));

    // Status mapping for legacy tasks (Portuguese to English)
    const statusMapping: { [key: string]: string } = {
      'Backlog': 'BACKLOG',
      'A Fazer': 'READY_TO_DEVELOP',
      'Em Progresso': 'IN_PROGRESS',
      'Em Revisão': 'IN_REVIEW',
      'Concluído': 'DONE'
    };

    // Reset all columns
    this.columns.forEach(column => column.tasks = []);

    // Organize tasks by status
    tasks.forEach(task => {
      console.log(`Processing task: "${task.title}" with status: "${task.status}"`);

      // Map legacy status to new status
      const mappedStatus = statusMapping[task.status] || task.status;
      console.log(`Mapped status: "${task.status}" -> "${mappedStatus}"`);

      const column = this.columns.find(col => col.id === mappedStatus);
      if (column) {
        column.tasks.push(task);
        console.log(`✓ Added to column: ${column.title}`);
      } else {
        console.log(`✗ No column found for mapped status: "${mappedStatus}"`);
        console.log('Available column IDs:', this.columns.map(c => c.id));
      }
    });

    console.log('Final column distribution:');
    this.columns.forEach(column => {
      console.log(`${column.title} (${column.id}): ${column.tasks.length} tasks`);
      if (column.tasks.length > 0) {
        column.tasks.forEach(task => {
          console.log(`  - ${task.title} (${task.status})`);
        });
      }
    });
    console.log('=== END ORGANIZE TASKS DEBUG ===');
  }

  getConnectedDropLists(): string[] {
    return this.columns.map((_, index) => `column-${index}`);
  }

  onDrop(event: CdkDragDrop<Task[]>): void {
    if (event.previousContainer === event.container) {
      // Reordering within the same column
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      // Moving between columns
      const task = event.previousContainer.data[event.previousIndex];
      const targetColumn = this.columns.find(col => col.tasks === event.container.data);
      
      if (targetColumn && task) {
        // Update task status
        this.updateTaskStatus(task, targetColumn.id);
        
        // Move the task in the UI
        transferArrayItem(
          event.previousContainer.data,
          event.container.data,
          event.previousIndex,
          event.currentIndex
        );
      }
    }
  }

  private updateTaskStatus(task: Task, newStatus: string): void {
    this.apiService.updateTaskStatus(task.id, { status: newStatus })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedTask) => {
          task.status = updatedTask.status;
        },
        error: (error) => {
          console.error('Error updating task status:', error);
          // Revert the UI change if API call fails
          this.loadData();
        }
      });
  }

  openTaskDetail(task: Task): void {
    this.selectedTask = task;
    this.isTaskDetailModalOpen = true;
  }

  closeTaskDetail(): void {
    this.selectedTask = null;
    this.isTaskDetailModalOpen = false;
  }

  openCreateTaskModal(): void {
    console.log('Opening create task modal');

    // Check if there are projects available
    if (this.projects.length === 0) {
      alert('Nenhum projeto disponível. Crie um projeto primeiro.');
      return;
    }

    this.newTask = {
      title: '',
      description: '',
      priority: TaskPriority.MEDIUM,
      status: TaskStatus.BACKLOG,
      projectId: 0,
      dueDate: '',
      assignedUserId: null
    };
    console.log('Reset form data:', this.newTask);
    console.log('Available projects:', this.projects);
    console.log('Available users:', this.users);
    this.isCreateTaskModalOpen = true;
  }

  closeCreateTaskModal(): void {
    this.isCreateTaskModalOpen = false;
  }

  createTask(): void {
    console.log('Creating task with data:', this.newTask);

    if (!this.isCreateTaskFormValid()) {
      console.error('Form validation failed:', {
        title: this.newTask.title,
        projectId: this.newTask.projectId,
        isValid: this.isCreateTaskFormValid()
      });
      return;
    }

    // Ensure the task has all required fields
    const taskData: CreateTaskRequest = {
      title: this.newTask.title.trim(),
      description: this.newTask.description?.trim() || '',
      priority: this.newTask.priority,
      status: TaskStatus.BACKLOG,
      projectId: Number(this.newTask.projectId),
      dueDate: this.newTask.dueDate && this.newTask.dueDate.trim() ? this.newTask.dueDate : undefined,
      assignedUserId: this.newTask.assignedUserId && this.newTask.assignedUserId > 0 ? this.newTask.assignedUserId : undefined
    };

    console.log('Sending task data to API:', taskData);

    this.apiService.createTask(taskData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (task) => {
          console.log('Task created successfully:', task);
          // Add task to backlog column
          const backlogColumn = this.columns.find(col => col.id === TaskStatus.BACKLOG);
          if (backlogColumn) {
            backlogColumn.tasks.push(task);
          }
          this.closeCreateTaskModal();
        },
        error: (error) => {
          console.error('Error creating task:', error);
          console.error('Error details:', {
            status: error.status,
            message: error.message,
            error: error.error
          });

          let errorMessage = 'Erro desconhecido';
          if (error.status === 403) {
            errorMessage = 'Você não tem permissão para criar tarefas neste projeto';
          } else if (error.status === 400) {
            errorMessage = 'Dados inválidos. Verifique se todos os campos obrigatórios estão preenchidos';
          } else if (error.status === 401) {
            errorMessage = 'Sessão expirada. Faça login novamente';
          } else if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }

          alert('Erro ao criar tarefa: ' + errorMessage);
        }
      });
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

  getConnectedLists(): string[] {
    return this.columns.map(column => `column-${column.id}`);
  }

  trackByTaskId(index: number, task: Task): number {
    return task.id;
  }

  trackByColumnId(index: number, column: KanbanColumn): string {
    return column.id;
  }

  isCreateTaskFormValid(): boolean {
    const isValid = this.newTask.title.trim().length > 0 &&
                   this.newTask.projectId > 0 &&
                   this.newTask.priority !== null &&
                   this.newTask.priority !== undefined;

    console.log('Form validation:', {
      title: this.newTask.title,
      titleValid: this.newTask.title.trim().length > 0,
      projectId: this.newTask.projectId,
      projectValid: this.newTask.projectId > 0,
      priority: this.newTask.priority,
      priorityValid: this.newTask.priority !== null && this.newTask.priority !== undefined,
      overallValid: isValid
    });

    return isValid;
  }
}
