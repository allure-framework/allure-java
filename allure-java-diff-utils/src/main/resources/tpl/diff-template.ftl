<#ftl output_format="HTML">
<#-- @ftlvariable name="data" type="io.qameta.allure.diff.AllureDiff.AllureDiffModel" -->

<head>
    <meta charset="UTF-8">
    <title>Diff</title>
    <style>
        .no-changes-footer {
            padding: 12px;
            margin: 8px;
            background: #e8f5e9;
            color: #2e7d32;
            border: 1px solid #c8e6c9;
            border-radius: 4px;
            font-family: sans-serif;
            font-size: 14px;
            text-align: center;
        }

        .diff-line {
            display: flex;
            font-family: monospace;
            white-space: pre;
            margin: 0;
            padding: 0;
            line-height: 1.5;
            font-size: 13px;
            align-items: center;
        }

        .line-number {
            width: 80px;
            flex-shrink: 0;
            color: #666;
            padding: 0 10px;
            text-align: right;
            background: #f5f5f5;
            height: 100%;
            box-sizing: border-box;
        }

        .original-line {
            border-right: 1px solid #eee;
        }

        .content {
            padding: 0 8px;
            flex-grow: 1;
            white-space: pre-wrap;
        }

        .ADDED {
            background-color: #ccffcc;
        }

        .REMOVED {
            background-color: #ffcccc;
        }

        .UNCHANGED {
            background-color: #f8f8f8;
        }

        .hidden {
            display: none !important;
        }

        body {
            margin: 0;
            padding: 0;
        }

        .controls label {
            margin-right: 12px;
            cursor: pointer;
            user-select: none;
        }

        .controls {
            display: flex;
            gap: 15px;
            align-items: center;
            margin-bottom: 8px;
            padding: 8px;
            background: #f8f8f8;
            border-bottom: 1px solid #ddd;
            font-family: sans-serif;
            font-size: 13px;
        }

        .controls button {
            padding: 4px 8px;
            background: #e0e0e0;
            border: 1px solid #ccc;
            border-radius: 3px;
            cursor: pointer;
            font-size: 13px;
        }

        .controls button:hover {
            background: #d0d0d0;
        }

        .copy-feedback {
            color: #008000;
            font-size: 12px;
            margin-left: 8px;
            display: none;
        }
    </style>
</head>

<body>
<div class="controls">
    <div class="toggle-group">
        <label>
            <input type="checkbox" checked onclick="toggleVisibility('ADDED')"> Added
        </label>
        <label>
            <input type="checkbox" checked onclick="toggleVisibility('REMOVED')"> Removed
        </label>
        <label>
            <input type="checkbox" checked onclick="toggleVisibility('UNCHANGED')"> Context
        </label>
        <label>
            <input type="checkbox" checked onclick="toggleLineNumbers()"> Numbers
        </label>
    </div>
    <div class="copy-group">
        <button onclick="copyText()">Copy</button>
        <span class="copy-feedback" id="copyFeedback"></span>
    </div>
</div>

<#assign hasChanges = false>
<#list data.rows() as line>
    <#if line.type().name() != "UNCHANGED">
        <#assign hasChanges = true>
        <#break>
    </#if>
</#list>

<#if !hasChanges>
    <div class="no-changes-footer">✓ Texts are identical - no differences found</div>
</#if>

<div class="diff-container">
    <#list data.rows() as line>
        <div class="diff-line ${line.type().name()}">
            <div class="line-number original-line"><#if line.originalLine() != -1>${line.originalLine()}<#else>-</#if></div>
            <div class="line-number revised-line"><#if line.revisedLine() != -1>${line.revisedLine()}<#else>-</#if></div>
            <div class="content">${line.text()}</div>
        </div>
    </#list>
</div>

<#noparse>
    <script>
        function showFeedback(message) {
            const feedback = document.getElementById('copyFeedback');
            feedback.textContent = message;
            feedback.style.display = 'inline';
            setTimeout(() => feedback.style.display = 'none', 2000);
        }

        function copyText() {
            const visibleContents = document.querySelectorAll('.diff-line:not(.hidden) .content');
            const lines = Array.from(visibleContents).map(content => content.innerText);

            navigator.clipboard.writeText(lines.join('\n'))
                .then(() => showFeedback('Text copied!'))
                .catch(err => console.error('Copy failed', err));
        }

        function toggleVisibility(className) {
            document.querySelectorAll(`.${className}`).forEach(element => {
                element.classList.toggle('hidden');
            });
        }

        function toggleLineNumbers() {
            document.querySelectorAll('.line-number').forEach(element => {
                element.classList.toggle('hidden');
            });
        }
    </script>
</#noparse>
</body>