// import { useMemo, useState } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { ArrowUpRight, ArrowRight, Bookmark, Search, MapPin, SlidersHorizontal, Check, BriefcaseBusiness, Clock, Upload, Sprout } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import hero from '@/assets/opportunities.jpg';
import { useRecruiting } from './store';
import { formatDate, type Job } from './data';
import { toast } from 'sonner';


type BackendJob = {
  id: number;
  title: string;
  companyName: string;
  location: string;
  employmentType: string;
  closingDate: string;
};

function convertBackendJob(job: BackendJob): Job {
  const typeMap: Record<string, string> = {
    FULL_TIME: 'Full-time',
    PART_TIME: 'Part-time',
    INTERNSHIP: 'Internship',
    CONTRACT: 'Contract',
  };

  return {
    id: job.id,
    title: job.title,
    company: job.companyName,
    logo: 'BT',
    color: 'green',
    location: job.location,
    type: typeMap[job.employmentType] ?? job.employmentType,
    mode: 'Not specified',
    salary: 'Not disclosed',
    category: 'Engineering',
    level: 'Not specified',
    posted: 'Date not available',
    closes: job.closingDate,
    status: 'Open',
    description: 'Please contact the recruiter for additional job details.',
  };
}


export function CompanyLogo({ job }: { job: Job }) { return <span className={`company-logo ${job.color}`} aria-label={job.company}>{job.logo === 'BU' ? <span className="text-base">BU</span> : job.logo}</span>; }
export function JobCard({ job, onSelect }: { job: Job; onSelect: (job: Job) => void }) {
  const { saved, toggleSave } = useRecruiting();
  return <article className="job-card"><div className="flex items-center justify-between"><div className="flex items-center gap-3"><CompanyLogo job={job} /><div><p className="text-xs font-semibold">{job.company}</p><p className="mt-1 text-[10px] text-muted-foreground">{job.posted}</p></div></div><Button variant="ghost" size="icon" aria-label={`${saved.includes(job.id) ? 'Unsave' : 'Save'} ${job.title}`} title={saved.includes(job.id) ? 'Remove from saved jobs' : 'Save job'} onClick={() => toggleSave(job.id)} className={saved.includes(job.id) ? 'text-primary' : 'text-muted-foreground'}><Bookmark fill={saved.includes(job.id) ? 'currentColor' : 'none'} className="size-[17px]" /></Button></div>
    <Button variant="link" onClick={() => onSelect(job)} className="job-title mt-5 h-auto justify-start whitespace-normal p-0 text-left text-[16px] font-semibold text-foreground no-underline hover:no-underline">{job.title}</Button><div className="mt-2.5 flex items-center gap-1.5 text-[11px] text-muted-foreground"><MapPin className="size-3" />{job.location}</div><div className="mt-4 flex flex-wrap gap-1.5"><span className="tag">{job.type}</span><span className="tag">{job.mode}</span><span className="tag">{job.level}</span></div><div className="job-card-footer mt-5 flex items-center justify-between gap-2 border-t pt-4"><div><p className="text-xs font-semibold">{job.salary}<span className="ml-1 text-[10px] font-normal text-muted-foreground">{job.salary.includes('hr') ? '' : '/ year'}</span></p><p className="mt-1 text-[9px] text-muted-foreground">Closes {formatDate(job.closes).replace(', 2026', '')}</p></div><Button variant="ghost" size="sm" onClick={() => onSelect(job)} className="h-8 gap-1.5 px-1 text-[11px] text-primary">View job <ArrowUpRight className="size-3.5" /></Button></div>
  </article>;
}



export function ExploreJobs({ savedOnly = false }: { savedOnly?: boolean }) {
  const { jobs, saved, role } = useRecruiting();

  const [backendJobs, setBackendJobs] = useState<Job[]>([]);
  const [loadingJobs, setLoadingJobs] = useState(true);
  const [jobsError, setJobsError] = useState('');

  useEffect(() => {
    if (savedOnly) {
      setLoadingJobs(false);
      return;
    }

    const controller = new AbortController();

    const loadJobs = async () => {
      setLoadingJobs(true);
      setJobsError('');

      try {
        const response = await fetch('/api/jobs/search/results', {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`Failed to load jobs (HTTP ${response.status})`);
        }

        const data: BackendJob[] = await response.json();

        setBackendJobs(data.map(convertBackendJob));
      } catch (error) {
        if (controller.signal.aborted) return;

        setJobsError(
            error instanceof Error
                ? error.message
                : 'Unable to load jobs.'
        );
      } finally {
        if (!controller.signal.aborted) {
          setLoadingJobs(false);
        }
      }
    };

    void loadJobs();

    return () => controller.abort();
  }, [savedOnly]);

  const displayedJobs = savedOnly ? jobs : backendJobs;

  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');
  const [location, setLocation] = useState('');
  const [type, setType] = useState('');
  const [category, setCategory] = useState('All opportunities');
  const [filterOpen, setFilterOpen] = useState(false);
  const [mode, setMode] = useState('');
  const [selected, setSelected] = useState<Job | null>(null);
  const [sort, setSort] = useState('recent');
  const categories = ['All opportunities', 'Design', 'Engineering', 'Marketing', 'Data & Analytics'];
  const matches = useMemo(() => displayedJobs.filter(j => (savedOnly ? saved.includes(j.id) : j.status === 'Open') && (!search || `${j.title} ${j.company}`.toLowerCase().includes(search.toLowerCase())) && (!location || j.location.includes(location)) && (!type || j.type === type) && (!mode || j.mode === mode) && (category === 'All opportunities' || category === j.category)).sort((a, b) => sort === 'closing' ? a.closes.localeCompare(b.closes) : a.id - b.id),
      [displayedJobs, saved, search, location, type, category, mode, savedOnly, sort]);
  const reset = () => { setKeyword(''); setSearch(''); setLocation(''); setType(''); setMode(''); setCategory('All opportunities'); };
  return <div className={`opportunities-layout ${savedOnly ? 'shortlist-layout' : 'explore-layout'}`}>
    {savedOnly ? <div className="mb-7"><p className="mb-2 text-xs text-primary">YOUR SHORTLIST</p><h1 className="page-heading">Saved opportunities</h1><p className="mt-2 text-sm text-muted-foreground">The roles you’re keeping an eye on, all in one place.</p></div> : <><div className="explore-intro"><div><p className="text-[12px] text-muted-foreground">{role === 'Guest' ? 'A fresh start begins here' : 'A little ambition goes a long way'}</p><h2 className="mt-1 text-[18px] font-semibold">{role === 'Guest' ? 'Welcome to Verdant.' : 'Make your next move, Alex.'}</h2></div><span className="hidden items-center gap-2 text-[10px] text-muted-foreground sm:flex"><span className="size-1.5 rounded-full bg-primary" /> Open to possibilities</span></div><section className="opportunity-banner"><img src={hero} alt="Wind turbines on green rolling hills" width={1920} height={768} /><div className="banner-copy"><span className="banner-eyebrow"><Sprout className="size-3.5" /> THE OPPORTUNITY EDIT</span><h1>Good work.<br />Great possibilities.</h1><p>Find your place in a team that moves you forward.</p></div><span className="banner-index"><span className="banner-index-number">
      {String(displayedJobs.filter(j => j.status === 'Open').length).padStart(2, '0')}
    </span><span>open roles<br />a fresh start awaits</span><ArrowUpRight className="size-5" /></span></section></>}
    <form onSubmit={e => { e.preventDefault(); setSearch(keyword); }} className="job-search"><div className="flex flex-1 items-center gap-2.5 px-3"><Search className="size-4 shrink-0 text-muted-foreground" /><input aria-label="Job title or company" placeholder="Job title or company" value={keyword} onChange={e => setKeyword(e.target.value)} className="h-9 w-full min-w-0 bg-transparent text-xs" /></div><div className="flex items-center gap-2 border-border px-3 sm:w-[190px] sm:border-l"><MapPin className="size-4 shrink-0 text-muted-foreground" /><select aria-label="Location" value={location} onChange={e => setLocation(e.target.value)} className="h-9 w-full bg-transparent text-xs"><option value="">Any location</option>{['San Francisco', 'New York', 'Waco', 'Austin', 'Boston'].map(l => <option key={l}>{l}</option>)}</select></div><div className="border-border px-3 sm:w-[150px] sm:border-l"><select aria-label="Employment type" value={type} onChange={e => setType(e.target.value)} className="h-9 w-full bg-transparent text-xs"><option value="">Job type</option><option>Full-time</option><option>Internship</option><option>Contract</option></select></div><Button type="submit" className="h-10 gap-3 text-xs">Search jobs <ArrowRight className="size-3.5" /></Button></form>
    <div className="category-bar flex flex-wrap items-center justify-between gap-3 border-b pb-4"><div className="flex max-w-full flex-wrap gap-1.5">{categories.map(c => <Button variant={category === c ? 'secondary' : 'ghost'} size="sm" key={c} onClick={() => setCategory(c)} className="h-8 px-3 text-[11px]">{c}{c === 'All opportunities' && <span className="ml-1 rounded bg-card/60 px-1.5 py-0.5 text-[9px]">
      {displayedJobs.filter(j =>
          savedOnly ? saved.includes(j.id) : j.status === 'Open'
      ).length}
    </span>}</Button>)}</div><Button variant="outline" size="sm" className="h-8 text-[11px]" onClick={() => setFilterOpen(!filterOpen)}><SlidersHorizontal className="size-3.5" />Filters{mode && <span className="size-1.5 rounded-full bg-primary" />}</Button></div>
    {filterOpen && <div className="flex flex-wrap items-center gap-4 border-b py-4 text-xs"><span className="font-medium">Workplace</span>{['Remote', 'Hybrid', 'On-site'].map(m => <label key={m} className="flex items-center gap-1.5"><input type="radio" name="workplace" checked={mode === m} onChange={() => setMode(m)} />{m}</label>)}<Button variant="link" size="sm" onClick={reset}>Reset filters</Button></div>}
    <div className="results-heading flex items-center justify-between py-5">
      {!savedOnly && loadingJobs && (
          <p className="py-3 text-sm text-muted-foreground">
            Loading jobs from server...
          </p>
      )}

      {!savedOnly && jobsError && (
          <p className="py-3 text-sm text-red-600" role="alert">
            {jobsError}
          </p>
      )}
      <p className="text-[12px]"><span className="font-semibold">{matches.length} opportunities</span><span className="ml-1 text-muted-foreground">{savedOnly ? 'saved for later' : 'to explore'}</span></p><label className="flex items-center gap-1.5 text-[10px] text-muted-foreground">Sort by:<select aria-label="Sort jobs" value={sort} onChange={e => setSort(e.target.value)} className="bg-transparent font-medium text-foreground"><option value="recent">Most recent</option><option value="closing">Closing soon</option></select></label></div>
    {/*{!loadingJobs && !jobsError && ({matches.length ? <div className="job-grid">*/}
    {/*  {matches.map(job => <JobCard job={job} key={job.id} onSelect={setSelected} />)}</div> : <div className="empty-state"><Search className="mx-auto mb-4 size-7" /><h3 className="text-base font-semibold text-foreground">{savedOnly && !saved.length ? 'Your shortlist is waiting' : 'No matching opportunities'}</h3><p className="mt-2 text-sm">{savedOnly && !saved.length ? 'Save a job that catches your eye.' : 'Try a different keyword or broaden your filters.'}</p><Button variant="outline" className="mt-5" onClick={reset}>Clear filters</Button></div>}*/}
    {/*{matches.length > 0 && <div className="mt-7 flex items-center justify-between text-[10px] text-muted-foreground"><span>Showing {matches.length} of {matches.length})} opportunities</span><div className="flex items-center gap-1"><Button size="icon" variant="outline" disabled aria-label="Previous page" className="size-7">←</Button><span className="flex size-7 items-center justify-center rounded bg-primary text-primary-foreground">1</span><Button size="icon" variant="outline" disabled aria-label="Next page" className="size-7">→</Button></div></div>}*/}

    {!loadingJobs && !jobsError && (
        <>
          {matches.length ? (
              <div className="job-grid">
                {matches.map(job => (
                    <JobCard
                        job={job}
                        key={job.id}
                        onSelect={setSelected}
                    />
                ))}
              </div>
          ) : (
              <div className="empty-state">
                <Search className="mx-auto mb-4 size-7" />
                <h3 className="text-base font-semibold text-foreground">
                  {savedOnly && !saved.length
                      ? 'Your shortlist is waiting'
                      : 'No matching opportunities'}
                </h3>
                <p className="mt-2 text-sm">
                  {savedOnly && !saved.length
                      ? 'Save a job that catches your eye.'
                      : 'Try a different keyword or broaden your filters.'}
                </p>
                <Button variant="outline" className="mt-5" onClick={reset}>
                  Clear filters
                </Button>
              </div>
          )}

          {matches.length > 0 && (
              <div className="mt-7 flex items-center justify-between text-[10px] text-muted-foreground">
        <span>
          Showing {matches.length} of {matches.length} opportunities
        </span>
                <div className="flex items-center gap-1">
                  <Button
                      size="icon"
                      variant="outline"
                      disabled
                      aria-label="Previous page"
                      className="size-7"
                  >
                    ←
                  </Button>
                  <span className="flex size-7 items-center justify-center rounded bg-primary text-primary-foreground">
            1
          </span>
                  <Button
                      size="icon"
                      variant="outline"
                      disabled
                      aria-label="Next page"
                      className="size-7"
                  >
                    →
                  </Button>
                </div>
              </div>
          )}
        </>
    )}

    <JobDetails job={selected} onClose={() => setSelected(null)} />
  </div>;
}




function JobDetails({ job, onClose }: { job: Job | null; onClose: () => void }) {
  const { applications, profile, role, toggleSave, saved, submit } = useRecruiting();
  const [step, setStep] = useState(0);
  const [resume, setResume] = useState('');
  const [cover, setCover] = useState('');
  const applied = job && applications.some(a => a.jobId === job.id && a.name === profile.name);
  const close = () => { setStep(0); setResume(''); onClose(); };
  return <Dialog open={Boolean(job)} onOpenChange={open => { if (!open) close(); }}><DialogContent className="max-h-[90vh] max-w-[660px] overflow-y-auto">{job && <><div className="flex items-center gap-3"><CompanyLogo job={job} /><div><p className="text-xs text-muted-foreground">{job.company}</p><DialogTitle className="mt-1 text-xl">{step ? 'Apply for ' : ''}{job.title}</DialogTitle></div></div><DialogDescription>{job.location} · {job.type} · {job.mode}</DialogDescription>{step === 0 ? <><div className="flex gap-2"><span className="tag">{job.salary}</span><span className="tag">{job.level}</span><span className={`status ${job.status.toLowerCase()}`}>{job.status}</span></div><section className="mt-3"><h3 className="section-heading">About the role</h3><p className="mt-3 text-sm leading-7 text-muted-foreground">{job.description}</p><h3 className="mt-5 text-sm font-semibold">What you’ll bring</h3><ul className="mt-2 list-disc space-y-2 pl-5 text-sm leading-6 text-muted-foreground"><li>Strong communication and collaboration skills.</li><li>Relevant experience or coursework in {job.category.toLowerCase()}.</li><li>A thoughtful approach to solving problems and learning new things.</li></ul><p className="mt-5 flex items-center gap-2 text-xs text-muted-foreground"><Clock className="size-4" /> Applications close {formatDate(job.closes)}.</p></section><div className="mt-4 flex flex-wrap gap-3 border-t pt-5"><Button disabled={Boolean(applied) || job.status !== 'Open'} onClick={() => { if (role !== 'Applicant') { toast.error('Choose the applicant workspace to preview an application.'); return; } setResume(profile.resume); setStep(1); }}>{applied ? <><Check />Already applied</> : job.status !== 'Open' ? 'Applications closed' : <>Apply now<ArrowUpRight /></>}</Button><Button variant="outline" onClick={() => toggleSave(job.id)}><Bookmark />{saved.includes(job.id) ? 'Saved' : 'Save job'}</Button></div></> : <form className="space-y-4" onSubmit={e => { e.preventDefault(); if (step === 1) { if (!resume) { toast.error('Please upload a PDF or DOCX resume.'); return; } setStep(2); } else if (submit(job, resume)) close(); }}><div className="flex items-center gap-3 border-y py-3 text-xs"><span className={step === 1 ? 'font-semibold text-primary' : 'text-muted-foreground'}>1. Application materials</span><ArrowRight className="size-3" /><span className={step === 2 ? 'font-semibold text-primary' : 'text-muted-foreground'}>2. Review & submit</span></div>{step === 1 ? <><div className="grid grid-cols-1 gap-3 sm:grid-cols-2"><label className="field">Full name<input required value={profile.name} readOnly /></label><label className="field">Email address<input required type="email" value={profile.email} readOnly /></label></div><div className="field">Resume<label className="cursor-pointer rounded-md border border-dashed p-5 text-center"><Upload className="mx-auto mb-2 size-5 text-primary" /><span className="block text-xs">{resume || 'Choose your resume'}</span><span className="mt-1 block text-[10px] text-muted-foreground">PDF or DOCX · Maximum 10 MB</span><input aria-label="Upload application resume" type="file" className="sr-only" accept=".pdf,.docx" onChange={e => { const file = e.target.files?.[0]; if (!file) return; if (!/\.(pdf|docx)$/i.test(file.name) || file.size > 10 * 1024 * 1024) { toast.error('Choose a PDF or DOCX file under 10 MB.'); return; } setResume(file.name); }} /></label></div><label className="field">Cover letter <span className="font-normal text-muted-foreground">Optional</span><textarea value={cover} onChange={e => setCover(e.target.value)} placeholder="What draws you to this opportunity?" /></label></> : <><div className="space-y-4 py-2 text-sm"><div><p className="text-xs text-muted-foreground">Applicant</p><p className="mt-1">{profile.name} · {profile.email}</p></div><div><p className="text-xs text-muted-foreground">Resume</p><p className="mt-1 flex items-center gap-2"><BriefcaseBusiness className="size-4" />{resume}</p></div>{cover && <div><p className="text-xs text-muted-foreground">Cover letter</p><p className="mt-1 whitespace-pre-wrap">{cover}</p></div>}</div><label className="flex items-start gap-2 text-xs leading-5"><input required type="checkbox" className="mt-1" />I confirm that my information is accurate and I’m ready to submit this application.</label></>}<div className="flex justify-between border-t pt-4"><Button type="button" variant="ghost" onClick={() => setStep(step - 1)}>Back</Button><Button type="submit">{step === 1 ? 'Review application' : 'Submit application'}<ArrowRight /></Button></div></form>}</>}</DialogContent></Dialog>;
}