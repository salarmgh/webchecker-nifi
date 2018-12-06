package me.salarmgh.processors.webchecker;


import me.salarmgh.PrometheusOperator.PrometheusOperator;
import me.salarmgh.PrometheusOperator.annotation.PrometheusAttribute;
import me.salarmgh.PrometheusOperator.annotation.PrometheusMetric;
import me.salarmgh.PrometheusOperator.annotation.PrometheusValue;

@PrometheusMetric(name = "webchecker")
public class WebCheckerCounter extends PrometheusOperator {

    public WebCheckerCounter(String job, String name, double value) {
        super(job);
        this.name = name;
        this.value = value;
    }


    private String name;
    private double value;

    @PrometheusAttribute(name = "name")
    public String getName() {
        return name;
    }


    @PrometheusValue(name = "value")
    public double getValue() {
        return value;
    }
}
