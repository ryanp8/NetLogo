package org.nlogo.properties

import scala.collection.mutable.ArrayBuffer

class MetricsBooleanEditor(accessor: PropertyAccessor[Boolean], properties: ArrayBuffer[PropertyEditor[_]]) extends BooleanEditor(accessor)
{
    override def changed(): Unit =
    {
        properties.find(_.accessor.accessString == "runMetricsCombine") match
        {
            case Some(p) => p.setEnabled(!get.getOrElse(true))
            case None =>
        }

        properties.find(_.accessor.accessString == "runMetricsConditions") match
        {
            case Some(p) => p.setEnabled(!get.getOrElse(true))
            case None =>
        }
    }
}