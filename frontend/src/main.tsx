import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router";
import Router from "./router/router.tsx";

createRoot(document.getElementById("root")!).render(
  <RouterProvider router={Router}></RouterProvider>
);
