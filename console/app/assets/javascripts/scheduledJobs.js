define(['knockout', 'jquery'], function (ko, $) {
    return function ScheduledJobsView() {
        var self = this;

        self.jobs = ko.observableArray();

        self._initialize = function() {
            $.get('/jobs', function (result) {
                console.log(JSON.stringify(result));
                $.each(result, function (item) {
                    self.jobs.push(item);
                });
            });
        };

        self._initialize();
    };
});