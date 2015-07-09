define ['superagent'], (superagent) ->
  ScheduledJobsView =
    scheduledJobs: (callback) ->
      superagent.get('/jobs').end (err, res) ->
        callback(res)