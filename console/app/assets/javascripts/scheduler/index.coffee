require ['knockout', 'SchedulerView', 'requirejs-domready!'], (ko, SchedulerView) ->
  ko.applyBindings(new SchedulerView())