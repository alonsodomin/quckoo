require ['knockout', 'ScheduledJobsView', 'requirejs-domready!'], (ko, ScheduledJobsView) ->
  ko.applyBindings(new ScheduledJobsView())