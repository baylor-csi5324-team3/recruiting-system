export type Role = 'Applicant' | 'Recruiter' | 'Administrator' | 'Guest';
export type Stage = 'Submitted' | 'Under review' | 'Interview' | 'Offer' | 'Accepted' | 'Declined' | 'Rejected' | 'Withdrawn';
export type Job = { id: number; title: string; company: string; logo: string; color: string; location: string; type: string; mode: string; salary: string; category: string; level: string; posted: string; closes: string; status: 'Open' | 'Draft' | 'Closed'; description: string; };
export type Application = { id: number; jobId: number; name: string; email: string; stage: Stage; date: string; resume: string; history: { stage: Stage; date: string; actor: string }[] };
export type Interview = { id: number; applicationId: number; date: string; time: string; location: string; notes: string; status: 'Scheduled' | 'Cancelled' };
export const formatDate = (date: string) => new Date(date + 'T12:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
export const stageClass = (stage: string) => stage === 'Under review' ? 'review' : stage.toLowerCase();
export const transitions: Partial<Record<Stage, Stage[]>> = { Submitted: ['Under review','Rejected'], 'Under review': ['Interview','Offer','Rejected'], Interview: ['Offer','Rejected'] };
