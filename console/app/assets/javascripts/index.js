require(['knockout', 'scheduledJobs'], function (ko, ScheduledJobsView) {
    console.log('Loading modules');
    ko.applyBindings(new ScheduledJobsView());
});