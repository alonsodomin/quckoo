require(['knockout', 'scheduledJobs', 'requirejs-domready!'], function (ko, ScheduledJobsView) {
    var view = new ScheduledJobsView();
    console.log(JSON.stringify(ScheduledJobsView));
    console.log(JSON.stringify(view));
    ko.applyBindings(view);
});