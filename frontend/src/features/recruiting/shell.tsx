import { useState, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from '@tanstack/react-router';
import { Search, Bookmark, FileText, UserRound, BriefcaseBusiness, UsersRound, ShieldCheck, PanelLeftClose, PanelLeftOpen, ArrowUpRight, ChevronDown, LogOut, Bell, Check, Sprout } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { useRecruiting } from './store';
import type { Role } from './data';

export function Brand() { return <><span className="brand-mark"><Sprout strokeWidth={2.3} /></span><span className="sidebar-wordmark font-display text-[25px] font-bold">verdant<span className="text-primary">.</span></span></>; }
// const applicantNav = [{ label: 'Explore jobs', to: '/' as const, icon: Search }, { label: 'Saved jobs', to: '/saved' as const, icon: Bookmark }, { label: 'My applications', to: '/applications' as const, icon: FileText }, { label: 'Interviews', to: '/interviews' as const, icon: CalendarDays }];
// const recruiterNav = [{ label: 'Job postings', to: '/postings' as const, icon: BriefcaseBusiness }, { label: 'Candidates', to: '/candidates' as const, icon: UsersRound }, { label: 'Interviews', to: '/interviews' as const, icon: CalendarDays }];

const applicantNav = [
  { label: 'Explore jobs', to: '/' as const, icon: Search },
  { label: 'Saved jobs', to: '/saved' as const, icon: Bookmark },
  { label: 'My applications', to: '/applications' as const, icon: FileText }
];

const recruiterNav = [
  { label: 'Job postings', to: '/postings' as const, icon: BriefcaseBusiness },
  { label: 'Candidates', to: '/candidates' as const, icon: UsersRound }
];

export function WorkspaceShell({ children }: { children: ReactNode }) {
  const { role, setRole, saved, applications, profile, audit } = useRecruiting();
  const [collapsed, setCollapsed] = useState(false);
  const [auth, setAuth] = useState(false);
  const [register, setRegister] = useState(false);
  const [authRole, setAuthRole] = useState<'Applicant' | 'Recruiter'>('Applicant');

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [companyName, setCompanyName] = useState('');

  const [registerLoading, setRegisterLoading] = useState(false);
  const [registerError, setRegisterError] = useState('');
  const [registerSuccess, setRegisterSuccess] = useState('');


  const pathname = useLocation({ select: l => l.pathname });
  const navigate = useNavigate();
  const nav = role === 'Recruiter' ? recruiterNav : role === 'Administrator' ? [{ label: 'User accounts', to: '/accounts' as const, icon: ShieldCheck }] : applicantNav;
  const name = role === 'Recruiter' ? 'Taylor Brooks' : role === 'Administrator' ? 'Robin Parker' : profile.name;
  // const changeRole = (next: Role) => {
  //   setRole(next);
  //   navigate({ to: next === 'Recruiter' ? '/postings' : next === 'Administrator' ? '/accounts' : '/' }); };

  const changeRole = (next: Role) => {
    setRole(next);
    navigate({
      to: next === 'Recruiter'
          ? '/postings'
          : next === 'Administrator'
              ? '/accounts'
              : '/'
    });
  };


  const handleRegister = async () => {
    setRegisterError('');
    setRegisterSuccess('');
    setRegisterLoading(true);

    try {
      const response = await fetch('/api/accounts/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: email.trim(),
          password,
          role: authRole.toUpperCase(),
          companyName: authRole === 'Recruiter'
              ? companyName.trim()
              : null,
          recruiterName: authRole === 'Recruiter'
              ? fullName.trim()
              : null,
        }),
      });

      if (!response.ok) {
        const message = await response.text();
        throw new Error(
            `Registration failed (${response.status}): ${message}`
        );
      }

      const result = await response.json();

      setRegisterSuccess(
          `Account created successfully! Account ID: ${result.accountId}`
      );

      setPassword('');
    } catch (error) {
      setRegisterError(
          error instanceof Error
              ? error.message
              : 'An unexpected error occurred.'
      );
    } finally {
      setRegisterLoading(false);
    }
  };

  const current = [...applicantNav, ...recruiterNav, { label: 'My profile', to: '/profile' }, { label: 'User accounts', to: '/accounts' }].find(n => n.to === pathname)?.label ?? 'Explore jobs';
  return <div className="app-shell">
    <aside className={`app-sidebar ${collapsed ? 'collapsed' : ''}`}>
      <Link to="/" className={`flex h-[79px] items-center gap-2.5 px-6 ${collapsed ? '[&_.sidebar-wordmark]:hidden' : ''}`} aria-label="Verdant home"><Brand /></Link>
      <div className="px-4 pt-7"><p className={`sidebar-label mb-3 px-3 text-[10px] font-semibold uppercase tracking-[1.4px] text-muted-foreground ${collapsed ? 'hidden' : ''}`}>{role === 'Recruiter' ? 'Hiring workspace' : role === 'Administrator' ? 'Administration' : 'Your workspace'}</p>
        <nav className="space-y-1">{nav.map(item => <Link key={item.label} to={item.to} className={`nav-item ${pathname === item.to ? 'active' : ''}`} title={item.label}><item.icon /><span className={`sidebar-label ${collapsed ? 'hidden' : ''}`}>{item.label}</span>{!collapsed && role === 'Applicant' && item.to === '/saved' && <span className="sidebar-label ml-auto text-[10px]">{saved.length}</span>}{!collapsed && role === 'Applicant' && item.to === '/applications' && <span className="sidebar-label ml-auto text-[10px]">{applications.filter(a => a.name === profile.name).length}</span>}</Link>)}</nav>
        {role === 'Applicant' && <><div className="my-6 border-t" /><Link to="/profile" title="My profile" className={`nav-item ${pathname === '/profile' ? 'active' : ''}`}><UserRound /><span className={`sidebar-label ${collapsed ? 'hidden' : ''}`}>My profile</span></Link></>}
      </div>
      <div className="mt-auto px-4 pb-4">
        {!collapsed && role === 'Applicant' && <div className="sidebar-extra mb-6 border-l-2 border-logo-gold bg-secondary/45 px-4 py-4"><div className="mb-2 flex items-center gap-2 text-xs font-semibold text-primary"><Sprout className="size-4" /> Your next chapter</div><p className="text-[11px] leading-5 text-muted-foreground">Good opportunities start with a great first impression.</p><Link to="/profile" className="mt-3 inline-flex items-center gap-2 text-[11px] font-semibold text-primary">Complete your profile <ArrowUpRight className="size-3.5" /></Link></div>}
        <div className="border-t pt-4"><DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" className={`h-auto w-full justify-start px-2 py-2 ${collapsed ? 'px-0' : ''}`}><span className="avatar">{name.split(' ').map(n => n[0]).join('')}</span>{!collapsed && <span className="sidebar-label min-w-0 text-left"><span className="block text-xs font-semibold">{role === 'Guest' ? 'Guest' : name}</span><span className="block text-[10px] font-normal text-muted-foreground">{role === 'Guest' ? 'Public workspace' : role}</span></span>}{!collapsed && <ChevronDown className="sidebar-label ml-auto size-3" />}</Button></DropdownMenuTrigger><DropdownMenuContent align="start"><DropdownMenuLabel>Preview workspace</DropdownMenuLabel>{(['Applicant', 'Recruiter', 'Administrator', 'Guest'] as Role[]).map(r => <DropdownMenuItem key={r} onClick={() => changeRole(r)}>{r}{role === r && <Check className="ml-auto size-3" />}</DropdownMenuItem>)}<DropdownMenuSeparator /><DropdownMenuItem onClick={() => changeRole('Guest')}><LogOut className="size-4" />Sign out</DropdownMenuItem></DropdownMenuContent></DropdownMenu></div>
      </div>
    </aside>
    <div className={`app-main ${collapsed ? 'collapsed' : ''}`}>
      <header className="topbar"><div className="flex min-w-0 items-center gap-3"><Button variant="ghost" size="icon" onClick={() => setCollapsed(!collapsed)} aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} className="hidden md:inline-flex">{collapsed ? <PanelLeftOpen /> : <PanelLeftClose />}</Button><span className="truncate text-[13px] font-medium">{current}</span></div><div className="flex items-center gap-4"><span className="hidden text-[11px] text-muted-foreground lg:block">Thursday, October 15</span><DropdownMenu><DropdownMenuTrigger asChild><Button size="icon" variant="ghost" aria-label="Notifications" title="Notifications" className="relative"><Bell className="size-[18px]" /><span className="absolute right-2 top-1.5 size-1.5 rounded-full bg-logo-gold" /></Button></DropdownMenuTrigger><DropdownMenuContent align="end" className="w-72"><DropdownMenuLabel>Recent activity</DropdownMenuLabel>{audit.length ? audit.slice(0, 4).map((a, i) => <div key={i} className="border-t px-3 py-3 text-xs leading-5">{a}</div>) : <div className="px-3 py-4 text-xs text-muted-foreground">Your upcoming interview is on October 22.</div>}</DropdownMenuContent></DropdownMenu>{role === 'Guest' ? <Button size="sm" onClick={() => setAuth(true)}>Sign in</Button> : <DropdownMenu><DropdownMenuTrigger asChild><Button variant="outline" size="sm" className="gap-2"><span className="hidden sm:inline">{role} view</span><span className="sm:hidden">{role}</span><ChevronDown className="size-3" /></Button></DropdownMenuTrigger><DropdownMenuContent align="end"><DropdownMenuLabel>Preview workspace</DropdownMenuLabel>{(['Applicant', 'Recruiter', 'Administrator', 'Guest'] as Role[]).map(r => <DropdownMenuItem key={r} onClick={() => changeRole(r)}>{r}{role === r && <Check className="ml-auto size-3" />}</DropdownMenuItem>)}</DropdownMenuContent></DropdownMenu>}</div></header>
      <main className="workspace">{children}</main>
      <footer className="mx-[38px] flex flex-wrap justify-between gap-2 border-t py-5 text-[10px] text-muted-foreground"><span>© 2026 Verdant · Recruiting & Application Management</span><span className="flex items-center gap-1.5"><span className="size-1.5 rounded-full bg-primary" /> Baylor University · Team 3</span></footer>
    </div>
    {/*<Dialog open={auth} onOpenChange={setAuth}>*/}
    {/*  <DialogContent>*/}
    {/*    <DialogTitle>*/}
    {/*      {register ? 'Create your account' : 'Welcome back'}*/}
    {/*  </DialogTitle>*/}
    {/*    <DialogDescription>*/}
    {/*      {register ? 'Choose your role to get started.' : 'Sign in to your recruiting workspace.'}*/}
    {/*    </DialogDescription>*/}
    {/*    <form className="space-y-4" onSubmit={e => { e.preventDefault(); changeRole(authRole); setAuth(false); }}>*/}
    {/*      {register && <label className="field">Full name<input required /></label>}*/}
    {/*      <label className="field">Email address<input required type="email" /></label>*/}
    {/*      <label className="field">Password<input required type="password" minLength={8} />*/}
    {/*      </label>*/}
    {/*      <label className="field">Role<select value={authRole} onChange={e => setAuthRole(e.target.value as 'Applicant' | 'Recruiter')}>*/}
    {/*        <option>Applicant</option><option>Recruiter</option></select></label>{authRole === 'Recruiter' && register && <label className="field">Company name<input required /></label>}<p className="text-xs text-muted-foreground">Frontend preview only. No account is created or authenticated.</p><Button type="submit" className="w-full">Continue to preview <ArrowUpRight /></Button>*/}
    {/*      <Button variant="link" type="button" className="w-full" onClick={() => setRegister(!register)}>{register ? 'Already have an account? Sign in' : 'New here? Create an account'}</Button></form>*/}
    {/*  </DialogContent>*/}
    {/*</Dialog>*/}

    <Dialog open={auth} onOpenChange={setAuth}>
      <DialogContent>
        <DialogTitle>
          {register ? 'Create your account' : 'Welcome back'}
        </DialogTitle>

        <DialogDescription>
          {register
              ? 'Choose your role to get started.'
              : 'Sign in to your recruiting workspace.'}
        </DialogDescription>

        <form
            className="space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();

              if (register) {
                await handleRegister();
              } else {
                // Existing frontend preview behavior.
                changeRole(authRole);
                setAuth(false);
              }
            }}
        >
          {register && (
              <label className="field">
                Full name
                <input
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                />
              </label>
          )}

          <label className="field">
            Email address
            <input
                required
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
            />
          </label>

          <label className="field">
            Password
            <input
                required
                type="password"
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />
          </label>

          <label className="field">
            Role
            <select
                value={authRole}
                onChange={(e) =>
                    setAuthRole(e.target.value as 'Applicant' | 'Recruiter')
                }
            >
              <option value="Applicant">Applicant</option>
              <option value="Recruiter">Recruiter</option>
            </select>
          </label>

          {authRole === 'Recruiter' && register && (
              <label className="field">
                Company name
                <input
                    required
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                />
              </label>
          )}

          {registerError && (
              <p className="text-sm text-red-600" role="alert">
                {registerError}
              </p>
          )}

          {registerSuccess && (
              <p className="text-sm text-green-700" role="status">
                {registerSuccess}
              </p>
          )}

          {!register && (
              <p className="text-xs text-muted-foreground">
                Frontend preview only. Sign in does not authenticate
                an account.
              </p>
          )}

          <Button
              type="submit"
              className="w-full"
              disabled={registerLoading}
          >
            {register
                ? registerLoading
                    ? 'Creating account...'
                    : 'Create account'
                : 'Continue to preview'}
            <ArrowUpRight />
          </Button>

          <Button
              variant="link"
              type="button"
              className="w-full"
              onClick={() => {
                setRegister(!register);
                setRegisterError('');
                setRegisterSuccess('');
              }}
          >
            {register
                ? 'Already have an account? Sign in'
                : 'New here? Create an account'}
          </Button>
        </form>
      </DialogContent>
    </Dialog>

  </div>;
}