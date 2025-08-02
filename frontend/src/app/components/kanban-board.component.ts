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
  
  columns: KanbanColumn[] = [
    {
      id: TaskStatus.BACKLOG,
      title: 'Backlog',
      icon: 'inbox',
      color: '#666666',
      tasks: []
    },
    {
      id: TaskStatus.READY_TO_DEVELOP,
      title: 'Pronto',
      icon: 'play',
      color: '#007AFF',
      tasks: []
    },
    {
      id: TaskStatus.IN_PROGRESS,
      title: 'Em Progresso',
      icon: 'spinner',
      color: '#FFC107',
      tasks: []
    },
    {
      id: TaskStatus.IN_REVIEW,
      title: 'Em Revisão',
      icon: 'eye',
      color: '#6f42c1',
      tasks: []
    },
    {
      id: TaskStatus.DONE,
      title: 'Concluído',
      icon: 'check',
      color: '#28A745',
      tasks: []
    }
  ];

  users: User[] = [];
  projects: Project[] = [];
  selectedTask: Task | null = null;
  isCreateTaskModalOpen = false;
  isTaskDetailModalOpen = false;
  loading = false;

  // Create Task Form
  newTask: CreateTaskRequest = {
    title: '',
    description: '',
    priority: TaskPriority.MEDIUM,
    project: { id: 0 }
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
    
    forkJoin({
      tasks: this.apiService.getTasks(),
      users: this.apiService.getUsers(),
      projects: this.apiService.getProjects()
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({ tasks, users, projects }) => {
        this.users = users;
        this.projects = projects;
        this.organizeTasks(tasks);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.loading = false;
      }
    });
  }

  private organizeTasks(tasks: Task[]): void {
    // Reset all columns
    this.columns.forEach(column => column.tasks = []);
    
    // Organize tasks by status
    tasks.forEach(task => {
      const column = this.columns.find(col => col.id === task.status);
      if (column) {
        column.tasks.push(task);
      }
    });
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

  private updateTaskStatus(task: Task, newStatus: TaskStatus): void {
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
    this.newTask = {
      title: '',
      description: '',
      priority: TaskPriority.MEDIUM,
      project: { id: 0 }
    };
    this.isCreateTaskModalOpen = true;
  }

  closeCreateTaskModal(): void {
    this.isCreateTaskModalOpen = false;
  }

  createTask(): void {
    if (!this.isCreateTaskFormValid()) {
      return;
    }

    this.apiService.createTask(this.newTask)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (task) => {
          // Add task to backlog column
          const backlogColumn = this.columns.find(col => col.id === TaskStatus.BACKLOG);
          if (backlogColumn) {
            backlogColumn.tasks.push(task);
          }
          this.closeCreateTaskModal();
        },
        error: (error) => {
          console.error('Error creating task:', error);
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

  trackByColumnId(index: number, column: KanbanColumn): TaskStatus {
    return column.id;
  }

  isCreateTaskFormValid(): boolean {
    return this.newTask.title.trim().length > 0 && this.newTask.project.id > 0;
  }
}
