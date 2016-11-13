/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.sparkdl.nn

import com.intel.analytics.sparkdl.tensor.Tensor
import com.intel.analytics.sparkdl.tensor.TensorNumericMath.TensorNumeric

import scala.reflect.ClassTag

/**
 *
 * A MultiLabel multiclass criterion based on sigmoid:

 * the loss is:
 * l(x,y) = - sum_i y[i] * log(p[i]) + (1 - y[i]) * log (1 - p[i])
 * where p[i] = exp(x[i]) / (1 + exp(x[i]))

 * and with weights:
 * l(x,y) = - sum_i weights[i] (y[i] * log(p[i]) + (1 - y[i]) * log (1 - p[i]))
 */
class MultiLabelSoftMarginCriterion[T: ClassTag]
(var weights: Tensor[T] = null, sizeAverage: Boolean = true)
  (implicit ev: TensorNumeric[T]) extends TensorCriterion[T] {
  var gradInput = Tensor[T]()
  var lsm = new Sigmoid[T]()
  var nll = new BCECriterion[T]()

  override def updateOutput(input: Tensor[T], target: Tensor[T]): T = {
    var _input: Tensor[T] = null
    var _target: Tensor[T] = null

    if (input.nElement() != 1) {
      _input = input.clone().squeeze()
    }

    if (target.nElement() != 1) {
      _target = target.clone().squeeze()
    }

    lsm.updateOutput(_input)
    nll.updateOutput(lsm.output, _target)
    output = nll.output

    output
  }

  override def updateGradInput(input: Tensor[T], target: Tensor[T]): Tensor[T] = {
    val size = input.size()
    var _input: Tensor[T] = null
    var _target: Tensor[T] = null

    if (input.nElement() != 1) {
      _input = input.clone().squeeze()
    }

    if (target.nElement() != 1) {
      _target = target.squeeze()
    }

    nll.updateGradInput(lsm.output, _target)
    lsm.updateGradInput(_input, nll.gradInput)
    gradInput = lsm.gradInput.view(size)

    gradInput
  }
}
