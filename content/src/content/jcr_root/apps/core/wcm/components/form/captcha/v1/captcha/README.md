<!--
Copyright 2020 Adobe

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
Form Captcha (v1)
====
Captcha component written in HTL.

## Features
* Provides support for Google reCAPTCHA V2 `invisible` and `checkbox`.

### Use Object
The Captcha component uses the `com.adobe.cq.wcm.core.components.models.form.Captcha` Sling Model for its Use-object.

### Edit Dialog Properties
The following properties are written to JCR for this Form Captcha component and are expected to be available as `Resource` properties:

1. `./id` - defines the component HTML ID attribute.

## Client Libraries
The component provides a `core.wcm.components.form.captcha.v1` client library category that contains a JavaScript component.
It should be added to a relevant site client library using the `embed` property.

## Information
* **Vendor**: Adobe
* **Version**: v1
* **Compatibility**: AEM 6.5
* **Status**: production-ready
* **Documentation**: [https://www.adobe.com/go/aem_cmp_form_captcha_v1](https://www.adobe.com/go/aem_cmp_form_captcha_v1)

