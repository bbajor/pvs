import * as echarts from 'echarts';

window.pvsECharts = {
    charts: new Map(),
    
    initChart(elementId, option) {
        const element = document.getElementById(elementId);
        if (!element) {
            console.error('ECharts element not found:', elementId);
            return;
        }
        
        // Entferne existierenden Chart falls vorhanden
        if (this.charts.has(elementId)) {
            this.charts.get(elementId).dispose();
        }
        
        // Erstelle neuen Chart
        const chart = echarts.init(element);
        chart.setOption(option);
        this.charts.set(elementId, chart);
        
        // Resize-Handler
        const resizeObserver = new ResizeObserver(() => {
            chart.resize();
        });
        resizeObserver.observe(element);
        
        return chart;
    },
    
    updateChart(elementId, option) {
        const chart = this.charts.get(elementId);
        if (chart) {
            chart.setOption(option, true);
        }
    },
    
    disposeChart(elementId) {
        const chart = this.charts.get(elementId);
        if (chart) {
            chart.dispose();
            this.charts.delete(elementId);
        }
    }
};











