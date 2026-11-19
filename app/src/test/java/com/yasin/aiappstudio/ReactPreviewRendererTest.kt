package com.yasin.aiappstudio

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReactPreviewRendererTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private fun createProject(): File {
        val workspace = temporaryFolder.newFolder("workspace")
        val project = File(workspace, "react-site").apply { mkdirs() }
        File(project, "package.json").writeText("""{"dependencies":{"react":"18.3.1","react-dom":"18.3.1"}}""")
        File(project, "src").mkdirs()
        File(project, "src/App.jsx").writeText("""import { useState } from 'react';
export default function App() {
  const [count, setCount] = useState(0);
  return <main><h1>Hello React</h1><button onClick={() => setCount(count + 1)}>{count}</button></main>;
}""")
        File(project, "src/style.css").writeText("body { color: navy; }")
        return workspace
    }

    @Test fun rendersReactStarterWithPinnedRuntimeAndRestrictedCsp() {
        val workspace = createProject()
        val html = ReactPreviewRenderer.render(workspace, "react-site")
        assertTrue(html.contains(ReactPreviewRenderer.REACT_URL))
        assertTrue(html.contains(ReactPreviewRenderer.REACT_DOM_URL))
        assertTrue(html.contains(ReactPreviewRenderer.BABEL_URL))
        assertTrue(html.contains("Babel.transform"))
        assertTrue(html.contains("ReactDOM.createRoot"))
        assertTrue(html.contains("connect-src 'none'"))
        assertTrue(html.contains("body { color: navy; }"))
        assertTrue(!html.contains("import { useState }"))
    }

    @Test fun onlyPinnedCdnAssetsAreAllowed() {
        assertTrue(ReactPreviewRenderer.isAllowedAsset(ReactPreviewRenderer.REACT_URL))
        assertTrue(ReactPreviewRenderer.isAllowedAsset(ReactPreviewRenderer.REACT_DOM_URL))
        assertTrue(ReactPreviewRenderer.isAllowedAsset(ReactPreviewRenderer.BABEL_URL))
        assertFalse(ReactPreviewRenderer.isAllowedAsset("https://example.com/app.js"))
        assertFalse(ReactPreviewRenderer.isAllowedAsset("https://cdn.jsdelivr.net/npm/react@latest/umd/react.js"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsImportsFromOtherFiles() {
        val workspace = createProject()
        File(workspace, "react-site/src/App.jsx").writeText("import Thing from './Thing.jsx'; export default function App(){ return <Thing/>; }")
        ReactPreviewRenderer.render(workspace, "react-site")
    }
}
