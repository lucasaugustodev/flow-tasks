import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../services/api.service';
import { 
  Task, 
  User, 
  TaskComment, 
  TaskChecklistItem,
  CreateCommentRequest,
  CreateChecklistItemRequest 
} from '../models/task.model';

@Component({
  selector: 'app-task-detail-modal',
  templateUrl: './task-detail-modal.component.html',
  styleUrls: ['./task-detail-modal.component.scss']
})
export class TaskDetailModalComponent implements OnInit, OnDestroy {
  @Input() task!: Task;
  @Input() users: User[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() taskUpdated = new EventEmitter<void>();

  private destroy$ = new Subject<void>();
  
  comments: TaskComment[] = [];
  checklistItems: TaskChecklistItem[] = [];
  activeTab: 'comments' | 'checklist' = 'comments';
  loading = false;
  
  newComment = '';
  newChecklistItem = '';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadTaskDetails();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTaskDetails(): void {
    this.loading = true;
    
    // Load comments and checklist
    this.apiService.getTaskComments(this.task.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (comments) => {
          this.comments = comments;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading comments:', error);
          this.loading = false;
        }
      });

    this.apiService.getTaskChecklist(this.task.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (items) => {
          this.checklistItems = items;
        },
        error: (error) => {
          console.error('Error loading checklist:', error);
        }
      });
  }

  closeModal(): void {
    this.close.emit();
  }

  setActiveTab(tab: 'comments' | 'checklist'): void {
    this.activeTab = tab;
  }

  // Comments
  addComment(): void {
    if (!this.canAddComment()) return;

    const request: CreateCommentRequest = {
      content: this.newComment.trim()
    };

    this.apiService.createTaskComment(this.task.id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (comment) => {
          this.comments.push(comment);
          this.newComment = '';
        },
        error: (error) => {
          console.error('Error adding comment:', error);
        }
      });
  }

  deleteComment(commentId: number): void {
    if (!confirm('Tem certeza que deseja excluir este comentário?')) return;

    this.apiService.deleteTaskComment(this.task.id, commentId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.comments = this.comments.filter(c => c.id !== commentId);
        },
        error: (error) => {
          console.error('Error deleting comment:', error);
        }
      });
  }

  // Checklist
  addChecklistItem(): void {
    if (!this.canAddChecklistItem()) return;

    const request: CreateChecklistItemRequest = {
      description: this.newChecklistItem.trim()
    };

    this.apiService.createChecklistItem(this.task.id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (item) => {
          this.checklistItems.push(item);
          this.newChecklistItem = '';
        },
        error: (error) => {
          console.error('Error adding checklist item:', error);
        }
      });
  }

  toggleChecklistItem(item: TaskChecklistItem): void {
    this.apiService.toggleChecklistItem(this.task.id, item.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedItem) => {
          const index = this.checklistItems.findIndex(i => i.id === item.id);
          if (index !== -1) {
            this.checklistItems[index] = updatedItem;
          }
        },
        error: (error) => {
          console.error('Error toggling checklist item:', error);
        }
      });
  }

  deleteChecklistItem(itemId: number): void {
    if (!confirm('Tem certeza que deseja excluir este item?')) return;

    this.apiService.deleteChecklistItem(this.task.id, itemId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.checklistItems = this.checklistItems.filter(i => i.id !== itemId);
        },
        error: (error) => {
          console.error('Error deleting checklist item:', error);
        }
      });
  }

  // Utility methods
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString('pt-BR');
  }

  formatDateShort(dateString: string): string {
    return new Date(dateString).toLocaleDateString('pt-BR');
  }

  getPriorityColor(priority: string): string {
    switch (priority) {
      case 'LOW':
        return '#28A745';
      case 'MEDIUM':
        return '#FFC107';
      case 'HIGH':
        return '#DC3545';
      case 'URGENT':
        return '#6f42c1';
      default:
        return '#666666';
    }
  }

  getPriorityLabel(priority: string): string {
    switch (priority) {
      case 'LOW':
        return 'Baixa';
      case 'MEDIUM':
        return 'Média';
      case 'HIGH':
        return 'Alta';
      case 'URGENT':
        return 'Urgente';
      default:
        return 'Média';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'BACKLOG':
        return 'Backlog';
      case 'READY_TO_DEVELOP':
        return 'Pronto';
      case 'IN_PROGRESS':
        return 'Em Progresso';
      case 'IN_REVIEW':
        return 'Em Revisão';
      case 'DONE':
        return 'Concluído';
      default:
        return status;
    }
  }

  getCompletedChecklistCount(): number {
    return this.checklistItems.filter(item => item.isCompleted).length;
  }

  getChecklistProgress(): number {
    if (this.checklistItems.length === 0) return 0;
    return (this.getCompletedChecklistCount() / this.checklistItems.length) * 100;
  }

  canAddComment(): boolean {
    return this.newComment.trim().length > 0;
  }

  canAddChecklistItem(): boolean {
    return this.newChecklistItem.trim().length > 0;
  }
}
