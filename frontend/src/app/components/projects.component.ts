import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../services/api.service';
import { Project, ProjectStatus } from '../models/task.model';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.scss']
})
export class ProjectsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  loading = false;
  searchTerm = '';
  selectedStatus: ProjectStatus | 'ALL' = 'ALL';
  
  isCreateModalOpen = false;
  selectedProject: Project | null = null;
  isDetailModalOpen = false;

  newProject = {
    name: '',
    description: '',
    status: ProjectStatus.PLANNING,
    startDate: '',
    endDate: ''
  };

  statusOptions = [
    { value: 'ALL', label: 'Todos os Status' },
    { value: ProjectStatus.PLANNING, label: 'Planejamento' },
    { value: ProjectStatus.ACTIVE, label: 'Ativo' },
    { value: ProjectStatus.ON_HOLD, label: 'Em Espera' },
    { value: ProjectStatus.COMPLETED, label: 'Concluído' },
    { value: ProjectStatus.CANCELLED, label: 'Cancelado' }
  ];

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadProjects(): void {
    this.loading = true;
    
    this.apiService.getProjects()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (projects) => {
          this.projects = projects;
          this.applyFilters();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading projects:', error);
          this.loading = false;
        }
      });
  }

  applyFilters(): void {
    let filtered = [...this.projects];

    // Filter by search term
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(project => 
        project.name.toLowerCase().includes(term) ||
        project.description.toLowerCase().includes(term)
      );
    }

    // Filter by status
    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(project => project.status === this.selectedStatus);
    }

    this.filteredProjects = filtered;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  openCreateModal(): void {
    this.newProject = {
      name: '',
      description: '',
      status: ProjectStatus.PLANNING,
      startDate: '',
      endDate: ''
    };
    this.isCreateModalOpen = true;
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  createProject(): void {
    if (!this.isCreateFormValid()) {
      return;
    }

    this.apiService.createProject(this.newProject)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (project) => {
          this.projects.push(project);
          this.applyFilters();
          this.closeCreateModal();
        },
        error: (error) => {
          console.error('Error creating project:', error);
        }
      });
  }

  openProjectDetail(project: Project): void {
    this.selectedProject = project;
    this.isDetailModalOpen = true;
  }

  closeProjectDetail(): void {
    this.selectedProject = null;
    this.isDetailModalOpen = false;
  }

  isCreateFormValid(): boolean {
    return this.newProject.name.trim().length > 0 && 
           this.newProject.description.trim().length > 0;
  }

  getStatusColor(status: ProjectStatus): string {
    switch (status) {
      case ProjectStatus.PLANNING:
        return '#6f42c1';
      case ProjectStatus.ACTIVE:
        return '#28A745';
      case ProjectStatus.ON_HOLD:
        return '#FFC107';
      case ProjectStatus.COMPLETED:
        return '#007AFF';
      case ProjectStatus.CANCELLED:
        return '#DC3545';
      default:
        return '#666666';
    }
  }

  getStatusLabel(status: ProjectStatus): string {
    switch (status) {
      case ProjectStatus.PLANNING:
        return 'Planejamento';
      case ProjectStatus.ACTIVE:
        return 'Ativo';
      case ProjectStatus.ON_HOLD:
        return 'Em Espera';
      case ProjectStatus.COMPLETED:
        return 'Concluído';
      case ProjectStatus.CANCELLED:
        return 'Cancelado';
      default:
        return status;
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('pt-BR');
  }

  getProjectDuration(project: Project): string {
    if (!project.startDate || !project.endDate) {
      return 'Duração não definida';
    }

    const start = new Date(project.startDate);
    const end = new Date(project.endDate);
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
}
