import { createFileRoute } from "@tanstack/react-router";
import { ExploreJobs } from '@/features/recruiting/jobs';
export const Route = createFileRoute("/")({
  head: () => ({ meta: [{ title: 'Explore Opportunities | Verdant Recruiting' }, { name: 'description', content: 'Find meaningful work with Verdant. Browse, filter, and save open roles across design, engineering, and more.' }, { property: 'og:title', content: 'Explore Opportunities | Verdant Recruiting' }, { property: 'og:description', content: 'Find your next chapter. Explore open roles and take the next step in your career.' }, { property: 'og:type', content: 'website' }, { name: 'twitter:card', content: 'summary_large_image' }] }),
  component: ExploreJobs,
});
