require ['knockout', 'scheduledJobs', 'domReady!'], (ko, ScheduledJobs) ->
  ko.applyBindings(new ScheduledJobs())