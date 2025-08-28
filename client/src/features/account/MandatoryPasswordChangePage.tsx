import { LoadingButton } from "@mui/lab";
import { 
  Container, 
  CssBaseline, 
  Box, 
  Avatar, 
  Typography, 
  TextField, 
  Button,
  IconButton,
  InputAdornment,
  List,
  ListItem,
  ListItemText,
  Paper
} from "@mui/material";
import { Visibility, VisibilityOff, LockOutlined, Info } from '@mui/icons-material';
import { useNavigate, useLocation } from "react-router-dom";
import { FieldValues, useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { useState, useEffect } from "react";
import agent from "../../app/api/agent";
import { useAppDispatch } from "../../app/store/configureStore";
import { logOut } from "./accountSlice";

interface PasswordRequirement {
  text: string;
  met: boolean;
}

export default function MandatoryPasswordChangePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showRepeatPassword, setShowRepeatPassword] = useState(false);
  const [requirements, setRequirements] = useState<PasswordRequirement[]>([]);
  const [passwordRequirements, setPasswordRequirements] = useState<string[]>([]);
  
  // Extract credentials from navigation state
  const credentials = location.state as { username?: string; password?: string } || {};
  
  // Redirect to login if credentials are not available
  useEffect(() => {
    if (!credentials.username || !credentials.password) {
      toast.error('Please login again to change your password');
      navigate('/login');
    }
  }, [credentials.username, credentials.password, navigate]);
  
  const {register, handleSubmit, formState: {isSubmitting, errors, isValid}, watch} = useForm({
    mode: 'onTouched'
  });

  const newPassword = watch('newPassword');
  const repeatPassword = watch('repeatPassword');

  useEffect(() => {
    // Fetch password requirements on component mount
    agent.Account.getPasswordRequirements()
      .then(response => {
        setPasswordRequirements(response.requirements || []);
      })
      .catch(error => {
        console.error('Failed to fetch password requirements:', error);
      });
  }, []);

  useEffect(() => {
    if (newPassword && passwordRequirements.length > 0) {
      // Validate password in real-time
      agent.Account.validatePassword(newPassword)
        .then(response => {
          const updatedRequirements = response.requirements?.map((req: string) => ({
            text: req.substring(2), // Remove ✓ or ✗ prefix
            met: req.startsWith('✓')
          })) || [];
          setRequirements(updatedRequirements);
        })
        .catch(error => {
          console.error('Password validation error:', error);
        });
    }
  }, [newPassword, passwordRequirements]);

  async function submitForm(data: FieldValues) {
    try {
      if (data.newPassword !== data.repeatPassword) {
        toast.error('Passwords do not match');
        return;
      }

      // Use the mandatory password change endpoint with credentials from navigation state
      await agent.Account.mandatoryPasswordChange({
        username: credentials.username,
        currentPassword: credentials.password,
        newPassword: data.newPassword,
        repeatPassword: data.repeatPassword
      });
      
      toast.success('Password changed successfully');
      navigate('/store');
    } catch (error) {
      console.error('Error changing password:', error);
      toast.error('Failed to change password. Please try again.');
    }
  }

  function handleLogout() {
    dispatch(logOut());
    navigate('/login');
  }

  const passwordsMatch = newPassword && repeatPassword && newPassword === repeatPassword;
  const allRequirementsMet = requirements.length > 0 && requirements.every(req => req.met);

  return (
    <Container component="main" maxWidth="sm">
      <CssBaseline />
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Avatar sx={{ m: 1, bgcolor: 'warning.main' }}>
          <LockOutlined />
        </Avatar>
        <Typography component="h1" variant="h5" sx={{ mb: 2 }}>
          Password Change Required
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
          Your password does not meet the current security requirements or has expired. 
          Please update your password to continue.
        </Typography>

        <Box component="form" onSubmit={handleSubmit(submitForm)} sx={{ width: '100%' }}>
          <TextField
            margin="normal"
            required
            fullWidth
            label="New Password"
            type={showNewPassword ? 'text' : 'password'}
            id="newPassword"
            autoComplete="new-password"
            {...register('newPassword', {required: 'New password is required'})}
            error={!!errors.newPassword}
            helperText={errors?.newPassword?.message as string}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    edge="end"
                  >
                    {showNewPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          <TextField
            margin="normal"
            required
            fullWidth
            label="Repeat New Password"
            type={showRepeatPassword ? 'text' : 'password'}
            id="repeatPassword"
            autoComplete="new-password"
            {...register('repeatPassword', {required: 'Please repeat your password'})}
            error={!!errors.repeatPassword || (repeatPassword && !passwordsMatch)}
            helperText={
              errors?.repeatPassword?.message as string || 
              (repeatPassword && !passwordsMatch ? 'Passwords do not match' : '')
            }
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowRepeatPassword(!showRepeatPassword)}
                    edge="end"
                  >
                    {showRepeatPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          {/* Password Requirements Tooltip */}
          <Paper elevation={1} sx={{ p: 2, mt: 2, mb: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Info color="primary" sx={{ mr: 1 }} />
              <Typography variant="subtitle2">Password Requirements:</Typography>
            </Box>
            <List dense>
              {requirements.map((req, index) => (
                <ListItem key={index} sx={{ py: 0 }}>
                  <ListItemText 
                    primary={req.text}
                    sx={{ 
                      color: req.met ? 'success.main' : 'text.secondary',
                      '& .MuiListItemText-primary': {
                        fontSize: '0.875rem'
                      }
                    }}
                  />
                  <Box sx={{ color: req.met ? 'success.main' : 'error.main' }}>
                    {req.met ? '✓' : '✗'}
                  </Box>
                </ListItem>
              ))}
            </List>
          </Paper>

          <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
            <Button
              variant="outlined"
              onClick={handleLogout}
              sx={{ flex: 1 }}
            >
              Logout
            </Button>
            <LoadingButton
              loading={isSubmitting}
              disabled={!allRequirementsMet || !passwordsMatch || !isValid}
              type="submit"
              variant="contained"
              sx={{ flex: 1 }}
            >
              Next
            </LoadingButton>
          </Box>
        </Box>
      </Box>
    </Container>
  );
}
