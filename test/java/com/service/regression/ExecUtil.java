package com.service.regression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ExecUtil
{
    /**
     * Tries to exec the command, waits for it to finish, logs errors if exit
     * status is nonzero, and returns true if exit status is 0 (success).
     *
     * @param command Description of the Parameter
     * @return Description of the Return Value
     */
    public static boolean exec(String[] command)
    {
        Process proc;

        try
        {
            proc = Runtime.getRuntime().exec(command);
        }
        catch (IOException e)
        {
            System.out.println("IOException while trying to execute " + command[0]);
            System.out.println(e.toString());
            return false;
        }

        // Create threads to read stdout and stderr
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "stdout");
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "stderr");
        outputGobbler.start();
        errorGobbler.start();

        int exitStatus;

        while (true)
        {
            try
            {
                exitStatus = proc.waitFor();
                break;
            }
            catch (java.lang.InterruptedException e)
            {
                System.out.println("Interrupted: Ignoring and waiting");
            }
        }
        if (exitStatus != 0)
        {
            System.out.println("Error executing command: " + exitStatus);
        }
        return (exitStatus == 0);
    }

    static class StreamGobbler extends Thread
    {
        private final InputStream is;
        private final String prefix;

        StreamGobbler(InputStream is, String prefix)
        {
            this.is = is;
            this.prefix = prefix;
        }

        @Override
        public void run()
        {
            InputStreamReader isr = null;
            BufferedReader br = null;

            try
            {
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null)
                {
                    System.out.println(prefix + ">" + line);
                }
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
            finally
            {
                if (isr != null)
                {
                    try
                    {
                        isr.close();
                    }
                    catch (IOException ioe)
                    {
                        // Ignore the exception here
                    }
                }
                if (br != null)
                {
                    try
                    {
                        br.close();
                    }
                    catch (IOException ioe)
                    {
                        // Ignore the exception here
                    }
                }
            }
        }
    }

}
