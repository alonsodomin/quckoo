define(['knockout', 'jquery'], function (ko, $) {
    function ScheduledJobsView() {
        this.jobs = ko.observableArray();
        this._initialize();
    }

    ScheduledJobsView.prototype = {
        _initialize: function () {
            var self = this;
            $.get('/jobs', function (result) {
                self.jobs.push(result);
            });
        }
    };

    return ScheduledJobsView;
});