function computeUsedAndFree(data) {
    // Remove the first entry (header)
    data.shift();
    // Sum values for non-"Free" entries (HashMap order is not guaranteed)
    var used = 0;
    var free = 0;
    data.forEach(function(entry) {
        if (entry[0].indexOf('Free') === 0) {
            free += entry[1];
        } else {
            used += entry[1];
        }
    });
    return { used: used, free: free };
}

function drawCpuChart(root, data) {
    var usage = computeUsedAndFree(data);
    var config = buildPieConfig('CPU Usage', usage.used, usage.free);
    var ctx = root.getElementById('cpu-usage-chart').getContext('2d');
    new Chart(ctx, config);
}

function drawMemoryChart(root, data) {
    var usage = computeUsedAndFree(data);
    var config = buildPieConfig('Memory Usage', usage.used, usage.free);
    var ctx = root.getElementById('mem-usage-chart').getContext('2d');
    new Chart(ctx, config);
}

function drawNodeCpuChart(root, nodeSafeId, data) {
    // Remove the first entry (header)
    data.shift();

    // Build a list of labels and values
    var labels = data.map(function(x) { return x[0]; });
    var values = data.map(function(x) { return x[1]; });

    var config = buildNodePieConfig(labels, values);
    var ctx = root.getElementById('cpu-' + nodeSafeId).getContext('2d');
    new Chart(ctx, config);
}

function drawNodeMemoryChart(root, nodeSafeId, data) {
    // Remove the first entry (header)
    data.shift();

    // Build a list of labels and values
    var labels = data.map(function(x) { return x[0]; });
    var values = data.map(function(x) { return x[1]; });

    var config = buildNodePieConfig(labels, values);
    var ctx = root.getElementById('memory-' + nodeSafeId).getContext('2d');
    new Chart(ctx, config);
}

function buildNodePieConfig(labels, values) {
    return {
        type: 'pie',
        data: {
            datasets: [{
                data: values,
                backgroundColor: ['red', 'green'],
                borderWidth: 1
            }],
            labels: labels
        },
        options: {
            responsive: false,
            maintainAspectRatio: false,
            title: {
                display: false,
                padding: 0
            },
            legend: {
                display: false
            },
            tooltips: {
                enabled: false
            },
            plugins: {
                datalabels: {
                    display: false
                }
            }
        }
    };
}

function buildPieConfig(title, usedValue, freeValue) {
    return {
        type: 'pie',
        data: {
            datasets: [{
                data: [usedValue, freeValue],
                backgroundColor: ['red', 'green'],
                borderWidth: 1,
                datalabels: {
                    anchor: 'center'
                }
            }],
            labels: ['Used', 'Free']
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            title: {
                display: true,
                text: title,
                padding: 0
            },
            legend: {
                display: true,
                labels: {
                    usePointStyle: true
                }
            },
            tooltips: {
                enabled: true
            },
            plugins: {
                datalabels: {
                    backgroundColor: function(context) {
                        return context.dataset.backgroundColor;
                    },
                    color: 'white',
                    padding: 2,
                    display: true,
                    font: {
                        weight: 'bold'
                    },
                    formatter: function(value, ctx) {
                        var sum = 0;
                        var dataArr = ctx.chart.data.datasets[0].data;
                        dataArr.forEach(function(d) { sum += d; });
                        return (value * 100 / sum).toFixed(1) + '%';
                    }
                }
            }
        }
    };
}
