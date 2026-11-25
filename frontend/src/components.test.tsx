import { render, screen, fireEvent, cleanup, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import App, { TaskCard, TaskForm } from './App'
import { api } from './api'
jest.mock('./api', () => ({ api: { get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn() } }))

afterEach(() => { cleanup(); localStorage.clear(); jest.clearAllMocks() })
describe('task components', () => {
  it('highlights an overdue task and lets the user change its status', () => {
    const onStatus = jest.fn()
    render(<TaskCard task={{ id: 1, title: 'Late work', description: null, status: 'TODO', priority: 'HIGH', dueDate: '2020-01-01', assignee: null }} onEdit={() => {}} onStatus={onStatus} />)
    expect(screen.getByText(/Overdue/)).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Move to'), { target: { value: 'DONE' } })
    expect(onStatus).toHaveBeenCalledWith('DONE')
  })
  it('shows a validation message for a blank title', async () => {
    const project = { id: 1, name: 'Project', description: null, owner: { id: 1, name: 'Alice', email: 'a@example.com' } }
    render(<QueryClientProvider client={new QueryClient()}><TaskForm project={project} task={null} close={() => {}} /></QueryClientProvider>)
    const title = screen.getByLabelText('Title') as HTMLInputElement
    expect(title.required).toBe(true)
    expect(title.validity.valid).toBe(false)
    fireEvent.click(screen.getByRole('button', { name: 'Save task' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Title is required')
    fireEvent.change(title, { target: { value: 'Prepare demo' } })
    await waitFor(() => expect(title.validity.valid).toBe(true))
  })
  it('navigates to protected projects after login', async () => {
    jest.mocked(api.post).mockResolvedValue({ data: { token: 'test-token', user: { id: 1, name: 'Alice' } } })
    jest.mocked(api.get).mockResolvedValue({ data: [] })
    render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><MemoryRouter initialEntries={['/login']}><App /></MemoryRouter></QueryClientProvider>)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'alice@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeTruthy()
    expect(localStorage.getItem('token')).toBe('test-token')
  })
})
