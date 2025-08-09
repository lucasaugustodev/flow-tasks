import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

interface ChatMessage { role: 'user' | 'assistant'; content: string; }

@Component({
  selector: 'app-ai-chat',
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.scss']
})
export class AiChatComponent {
  messages: ChatMessage[] = [
    { role: 'assistant', content: 'Olá! Sou sua assistente de projetos. Posso criar tarefas, mover no Kanban e criar projetos. Como posso ajudar?' }
  ];
  input = '';
  loading = false;

  constructor(private http: HttpClient) {}

  async send() {
    const text = this.input.trim();
    if (!text || this.loading) return;
    this.messages.push({ role: 'user', content: text });
    this.input = '';
    this.loading = true;

    try {
      const resp: any = await this.http.post(`${environment.apiUrl}/ai/chat`, { message: text }).toPromise();
      const reply = resp?.message || '(sem resposta)';
      this.messages.push({ role: 'assistant', content: reply });
    } catch (e: any) {
      this.messages.push({ role: 'assistant', content: 'Ocorreu um erro ao falar com a IA.' });
    } finally {
      this.loading = false;
    }
  }
}

