/*******************************************************************************
 * Copyright 2020 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
(function() {
    'use strict';

    /**
     * Selector for captcha components.
     *
     * @type {string}
     */
    const CAPTCHA_COMPONENT_SELECTOR = 'form [data-cmp-is=formCaptcha]';

    /**
     * Selector for the CAPTCHA widget container within the component.
     *
     * @type {string}
     */
    const WIDGET_CONTAINER_SELECTOR = '.g-recaptcha[data-sitekey]'

    /**
     * Selector for the error message block within the component.
     *
     * @type {string}
     */
    const INVALID_MESSAGE_CONTAINER_SELECTOR = '.cpm-form-captcha__error-msg';

    /**
     * Loads the specified JS and fulfils the promise when load is complete.
     *
     * @param uri The URI of the JS file to load.
     * @returns {Promise} Promise that is resolved when the JS has loaded.
     */
    const loadScriptAsync = function(uri) {
        return new Promise((resolve) => {
            const tag = document.createElement('script');
            tag.src = uri;
            tag.async = true;
            tag.onload = resolve;
            const firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
        });
    };

    /**
     * loads the reCAPTCHA API and waits until it is ready.
     * If the reCaptcha API is already available, then the returned promise resolves immediately.
     *
     * @returns {Promise<unknown>} Promised resolved when `grecaptcha` is ready.
     */
    const loadRecaptchaAPI = function() {
        return new Promise((resolve) => {
            if (typeof grecaptcha == 'undefined') {
                loadScriptAsync('https://www.google.com/recaptcha/api.js?render=explicit')
                    .then(() => new Promise((resolve) => grecaptcha.ready(() => resolve(grecaptcha))))
                    .then(resolve);
            } else {
                resolve(grecaptcha);
            }
        });
    }

    /**
     * Initializes the CAPTCHA.
     *
     * @param config The CAPTCHA configuration.
     * @constructor
     */
    function FormCaptcha(config) {
        this.element = config.element;
        this.protectedForm = config.element.closest('form');
        this.captchaContainer = config.element.querySelector(WIDGET_CONTAINER_SELECTOR);
        this.invalidContainer = config.element.querySelector(INVALID_MESSAGE_CONTAINER_SELECTOR)

        // prevents multiple initialization
        this.element.removeAttribute('data-cmp-is');

        // only continue initializing if this element is inside a form and has a captcha container
        if (this.protectedForm != null && this.captchaContainer != null) {
            if (this.element.dataset.keytype === 'recaptcha-v2-invisible') {
                this._initializeV2Invisible();
            } else {
                this._initializeV2Checkbox();
            }
        }
    }

    /**
     * Function for initializing a reCAPTCHA V2 Checkbox.
     *
     * @private
     */
    FormCaptcha.prototype._initializeV2Checkbox = function() {
        // creates an observer to detect the creation of `g-recaptcha-response` and make it required.
        new MutationObserver((mutations) => {
            mutations.flatMap((mutation) => [].slice.call(mutation.addedNodes))
                .filter((addedNode) => addedNode.querySelectorAll)
                .flatMap((addedNode) => [].slice.call(addedNode.querySelectorAll('[name=g-recaptcha-response]')))
                .forEach((inputElement) => {
                    inputElement.setAttribute('required', 'required')
                    inputElement.addEventListener('invalid', this._onInvalidV2Checkbox.bind(this));
                })
        }).observe(this.element, {subtree: true, childList: true, characterData: true});

        // render the widget
        this.widgetID = grecaptcha.render(this.captchaContainer, {
            callback: this._onValidV2Checkbox.bind(this)
        }, true);
    }

    /**
     * Function for initializing a reCAPTCHA V2 Invisible.
     *
     * @private
     */
    FormCaptcha.prototype._initializeV2Invisible = function() {
        // render the widget
        this.widgetID = grecaptcha.render(this.captchaContainer, {}, true);

        // handle invisible CAPTCHA on form submit
        this.submitListener = this._v2InvisibleSubmitCallback.bind(this);
        this.protectedForm.addEventListener('submit', this.submitListener, false);
    }

    /**
     * Callback to be bound to form submissions when using Captcha V2 - Invisible.
     * This ensures that the captcha check is invoked on form submission.
     *
     * @param event The form event.
     * @returns {boolean}
     * @private
     */
    FormCaptcha.prototype._v2InvisibleSubmitCallback = function(event) {
        event.preventDefault();
        grecaptcha.execute(this.widgetID).then((token) => {
            this.protectedForm.removeEventListener('submit', this.submitListener);
            this.protectedForm.submit();
        });
        return false;
    }

    /**
     * Callback for when a reCAPTCHA V2 Checkbox is checked.
     * This function hides any error message indicating that the field must be completed.
     *
     * @param token The response token.
     * @returns {Promise<unknown>} Resolved promise with the token as it's parameter.
     * @private
     */
    FormCaptcha.prototype._onValidV2Checkbox = function(token) {
        return new Promise((resolve) => {
            if (token) {
                this.invalidContainer.style.display = 'none';
            }
            return resolve(token);
        });
    }

    /**
     * Callback for when a form containing an unchecked reCAPTCHA v2 Checkbox is submitted.
     * This function un-hides the error message container to show the requirement text.
     *
     * Note: the error message container is only required because the captcha token form element
     * is hidden and not focusable, and therefore HTML5 validation cannot be used.
     *
     * @param event The submit event.
     * @private
     */
    FormCaptcha.prototype._onInvalidV2Checkbox = function(event) {
        event.preventDefault();
        this.invalidContainer.style.removeProperty('display');
    }

    /**
     * Function to initialize all CAPTCHAs.
     */
    const initialize = function() {
        // load all existing captchas
        const captchas = [].slice.call(document.querySelectorAll(CAPTCHA_COMPONENT_SELECTOR))
            .filter(node => node.querySelector(WIDGET_CONTAINER_SELECTOR))
        if (captchas.length > 0) {
            loadRecaptchaAPI().then(() => captchas.forEach((element) => new FormCaptcha({element: element})));
        }
    };

    /**
     * Creates a mutation observer that initializes any newly added CAPTCHAs.
     */
    const registerObservers = function() {
        new MutationObserver(initialize)
            .observe(document.querySelector('body'), {
                subtree: true,
                childList: true,
                characterData: true
        });
    }

    // initialize when ready
    new Promise((resolve) => document.readyState !== 'loading'
        ? resolve(document)
        : document.addEventListener('DOMContentLoaded', () => resolve(document))
    ).then(initialize).then(registerObservers);

}());
