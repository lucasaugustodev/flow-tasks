import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login.component';
import { DashboardComponent } from './components/dashboard.component';
import { ProjectsComponent } from './components/projects.component';
import { ProjectDetailComponent } from './components/project-detail.component';
import { KanbanBoardComponent } from './components/kanban-board.component';
import { AuthGuard } from './guards/auth.guard';
import { AiChatComponent } from './components/ai-chat.component';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'projects', component: ProjectsComponent, canActivate: [AuthGuard] },
  { path: 'projects/:id', component: ProjectDetailComponent, canActivate: [AuthGuard] },
  { path: 'kanban', component: KanbanBoardComponent, canActivate: [AuthGuard] },
  { path: 'ai-chat', component: AiChatComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
